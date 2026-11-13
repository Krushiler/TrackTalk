package top.krushiler.tracktalk.presentation.screens.location

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tracktalk.app.shared.generated.resources.Res
import tracktalk.app.shared.generated.resources.back
import tracktalk.app.shared.generated.resources.location_history
import tracktalk.app.shared.generated.resources.location_history_apply
import tracktalk.app.shared.generated.resources.location_history_clear_to
import tracktalk.app.shared.generated.resources.location_history_from
import tracktalk.app.shared.generated.resources.location_history_invalid_range
import tracktalk.app.shared.generated.resources.location_history_last_hour
import tracktalk.app.shared.generated.resources.location_history_range_label
import tracktalk.app.shared.generated.resources.location_history_range_title
import tracktalk.app.shared.generated.resources.location_history_to
import tracktalk.app.shared.generated.resources.location_history_to_now
import tracktalk.app.shared.generated.resources.location_history_until_now
import tracktalk.app.shared.generated.resources.no_location_history
import tracktalk.app.shared.generated.resources.retry
import top.krushiler.tracktalk.geo.GeoPoint
import top.krushiler.tracktalk.platform.formatLocalDateTime
import top.krushiler.tracktalk.presentation.navigation.PersonRef
import top.krushiler.tracktalk.presentation.theme.AppColors
import top.krushiler.tracktalk.presentation.theme.AppIcons
import top.krushiler.tracktalk.presentation.theme.GradientButton
import top.krushiler.tracktalk.presentation.util.displayName
import top.krushiler.tracktalk.presentation.widgets.FriendPin
import top.krushiler.tracktalk.presentation.widgets.MapCamera
import top.krushiler.tracktalk.presentation.widgets.MapMarker
import top.krushiler.tracktalk.presentation.widgets.MapOverlay
import top.krushiler.tracktalk.presentation.widgets.TrackMap
import top.krushiler.tracktalk.presentation.widgets.fitCamera

@Composable
fun LocationHistoryRoute(
    person: PersonRef,
    onBack: () -> Unit,
    viewModel: LocationHistoryViewModel = koinViewModel { parametersOf(person) },
) {
    val state by viewModel.uiState.collectAsState()
    LocationHistoryScreen(
        state = state,
        onBack = onBack,
        onRange = viewModel::changeRange,
        onSelect = viewModel::select,
        onRetry = viewModel::load,
    )
}

@Composable
fun LocationHistoryScreen(
    state: LocationHistoryUiState,
    onBack: () -> Unit,
    onRange: (Instant, Instant?) -> Unit,
    onSelect: (top.krushiler.tracktalk.model.LocationLogEntry?) -> Unit,
    onRetry: () -> Unit,
) {
    val name = displayName(state.person.username, state.person.login, state.person.userId)
    var showRange by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf(MapCamera(37.7749, -122.4194, 13.0)) }
    var move by remember { mutableIntStateOf(0) }
    val points = state.entries.map { GeoPoint(it.latitude, it.longitude) }
    val livePoint = state.live?.let { GeoPoint(it.latitude, it.longitude) }
    val fitPoints = points + listOfNotNull(livePoint)
    val fitKey = state.entries.map { it.id } to livePoint
    Box(Modifier.fillMaxSize()) {
        TrackMap(
            camera = camera,
            onCameraChange = { camera = it },
            markers = listOfNotNull(livePoint?.let { MapMarker(state.person.userId, it) }),
            overlay = MapOverlay(
                polyline = points + listOfNotNull(livePoint),
                dots = points,
                selected = state.selected?.let { GeoPoint(it.latitude, it.longitude) },
            ),
            move = move,
            onMapClick = { tap ->
                val nearest = state.entries.minByOrNull { entry ->
                    val dx = entry.longitude - tap.longitude
                    val dy = entry.latitude - tap.latitude
                    dx * dx + dy * dy
                }
                onSelect(nearest)
            },
            markerContent = {
                state.live?.let { live -> FriendPin(friend = live, selected = true) }
            },
        )
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.96f))
                .statusBarsPadding()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    AppIcons.chevronLeft,
                    contentDescription = stringResource(Res.string.back),
                    tint = AppColors.primary,
                    modifier = Modifier.size(40.dp).clickable(onClick = onBack).padding(6.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(name, color = AppColors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(stringResource(Res.string.location_history), color = AppColors.muted, fontSize = 12.sp)
                }
            }
            Text(
                text = rangeLabel(state.from, state.to),
                modifier = Modifier
                    .padding(start = 12.dp, top = 4.dp)
                    .clickable { showRange = true },
                color = AppColors.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = AppColors.primary)
            state.error == "invalid-range" -> Text(
                stringResource(Res.string.location_history_invalid_range),
                modifier = Modifier.align(Alignment.Center).background(Color.White).padding(12.dp),
                color = AppColors.danger,
            )
            state.error != null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error, color = AppColors.danger, modifier = Modifier.padding(16.dp))
                TextButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
            }
            state.entries.isEmpty() && livePoint == null -> Text(
                stringResource(Res.string.no_location_history),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp),
                color = AppColors.muted,
            )
        }
        state.selected?.let { entry ->
            Text(
                text = "${formatLocalDateTime(entry.recordedAt.toEpochMilliseconds())}  ${entry.latitude.toString().take(8)}, ${entry.longitude.toString().take(8)}",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = AppColors.text,
                fontSize = 12.sp,
            )
        }
    }
    LaunchedEffect(fitKey) {
        if (fitPoints.isNotEmpty()) {
            camera = fitCamera(fitPoints, 800f, 800f)
            move += 1
        }
    }
    if (showRange) {
        RangeSheet(
            from = state.from,
            to = state.to,
            onDismiss = { showRange = false },
            onApply = { from, to ->
                showRange = false
                onRange(from, to)
            },
        )
    }
}

@Composable
private fun rangeLabel(from: Instant, to: Instant?): String {
    val start = formatLocalDateTime(from.toEpochMilliseconds())
    return if (to == null) {
        stringResource(Res.string.location_history_until_now, start)
    } else {
        stringResource(Res.string.location_history_range_label, start, formatLocalDateTime(to.toEpochMilliseconds()))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeSheet(
    from: Instant,
    to: Instant?,
    onDismiss: () -> Unit,
    onApply: (Instant, Instant?) -> Unit,
) {
    var draftFrom by remember { mutableStateOf(from) }
    var draftTo by remember { mutableStateOf(to) }
    var picking by remember { mutableStateOf<Boolean?>(null) }
    val picker = rememberDatePickerState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = Color.White,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.location_history_range_title), color = AppColors.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            TextButton(onClick = {
                onApply(Clock.System.now() - 1.hours, null)
            }) {
                Icon(AppIcons.schedule, contentDescription = null, tint = AppColors.primary)
                Text(stringResource(Res.string.location_history_last_hour), modifier = Modifier.padding(start = 8.dp))
            }
            TextButton(onClick = { picking = true }) {
                Text("${stringResource(Res.string.location_history_from)}: ${formatLocalDateTime(draftFrom.toEpochMilliseconds())}")
            }
            TextButton(onClick = { picking = false }) {
                val label = draftTo?.let { formatLocalDateTime(it.toEpochMilliseconds()) } ?: stringResource(Res.string.location_history_to_now)
                Text("${stringResource(Res.string.location_history_to)}: $label")
            }
            if (picking != null) {
                DatePicker(state = picker)
                TextButton(onClick = {
                    val millis = picker.selectedDateMillis
                    if (millis != null) {
                        val instant = Instant.fromEpochMilliseconds(millis)
                        if (picking == true) draftFrom = instant else draftTo = instant
                    }
                    picking = null
                }) {
                    Text(stringResource(Res.string.location_history_apply))
                }
            }
            TextButton(onClick = { draftTo = null }) { Text(stringResource(Res.string.location_history_clear_to)) }
            val invalid = draftTo != null && draftFrom >= draftTo!!
            if (invalid) {
                Text(stringResource(Res.string.location_history_invalid_range), color = AppColors.danger, fontSize = 13.sp)
            }
            GradientButton(
                label = stringResource(Res.string.location_history_apply),
                onClick = { onApply(draftFrom, draftTo) },
                enabled = !invalid,
            )
        }
    }
}
