package top.krushiler.tracktalk.presentation.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tracktalk.app.shared.generated.resources.Res
import tracktalk.app.shared.generated.resources.away
import tracktalk.app.shared.generated.resources.close
import tracktalk.app.shared.generated.resources.friends_on_map
import tracktalk.app.shared.generated.resources.latitude
import tracktalk.app.shared.generated.resources.location_history
import tracktalk.app.shared.generated.resources.longitude
import tracktalk.app.shared.generated.resources.navigate
import tracktalk.app.shared.generated.resources.search_friends
import top.krushiler.tracktalk.geo.GeoPoint
import top.krushiler.tracktalk.model.FriendLocation
import top.krushiler.tracktalk.platform.LocationAccess
import top.krushiler.tracktalk.platform.openExternalUrl
import top.krushiler.tracktalk.presentation.navigation.PersonRef
import top.krushiler.tracktalk.presentation.navigation.toPerson
import top.krushiler.tracktalk.presentation.screens.home.LocationSyncUiState
import top.krushiler.tracktalk.presentation.screens.home.LocationSyncViewModel
import top.krushiler.tracktalk.presentation.screens.home.MapUiState
import top.krushiler.tracktalk.presentation.screens.home.MapViewModel
import top.krushiler.tracktalk.presentation.theme.AppColors
import top.krushiler.tracktalk.presentation.theme.AppIcons
import top.krushiler.tracktalk.presentation.util.avatarColor
import top.krushiler.tracktalk.presentation.util.displayName
import top.krushiler.tracktalk.presentation.util.distanceLabel
import top.krushiler.tracktalk.presentation.util.initialsOf
import top.krushiler.tracktalk.presentation.util.personName
import top.krushiler.tracktalk.presentation.util.relativeTime
import top.krushiler.tracktalk.presentation.widgets.AppAvatar
import top.krushiler.tracktalk.presentation.widgets.FriendPin
import top.krushiler.tracktalk.presentation.widgets.LocationPermissionCard
import top.krushiler.tracktalk.presentation.widgets.MapCamera
import top.krushiler.tracktalk.presentation.widgets.MapMarker
import top.krushiler.tracktalk.presentation.widgets.MapOverlay
import top.krushiler.tracktalk.presentation.widgets.SheetHandle
import top.krushiler.tracktalk.presentation.widgets.TrackMap

@Composable
fun MapRoute(
    locationSync: LocationSyncViewModel,
    onOpenHistory: (PersonRef) -> Unit,
    viewModel: MapViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val sync by locationSync.uiState.collectAsState()
    val access by locationSync.permission.collectAsState()
    MapScreen(
        state = state,
        sync = sync,
        access = access,
        onAllowLocation = locationSync::requestPermission,
        onOpenSettings = locationSync::openSettings,
        onOpenHistory = onOpenHistory,
    )
}

@Composable
fun MapScreen(
    state: MapUiState,
    sync: LocationSyncUiState,
    access: LocationAccess,
    onAllowLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: (PersonRef) -> Unit,
) {
    var camera by remember { mutableStateOf(MapCamera(37.7749, -122.4194, 13.0)) }
    var move by remember { mutableIntStateOf(0) }
    var satellite by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<FriendLocation?>(null) }
    var centered by remember { mutableStateOf(false) }
    val self = sync.location
    LaunchedEffect(self) {
        val location = self ?: return@LaunchedEffect
        if (!centered) {
            centered = true
            camera = MapCamera(location.latitude, location.longitude, 15.0)
            move += 1
        }
    }
    val filtered = remember(query, state.friends) {
        val text = query.trim().lowercase()
        if (text.isEmpty()) emptyList()
        else state.friends.filter { personName(it.user.username, it.user.login).lowercase().contains(text) }
    }
    Box(Modifier.fillMaxSize()) {
        TrackMap(
            camera = camera,
            onCameraChange = { camera = it },
            satellite = satellite,
            markers = state.friends.map { MapMarker(it.user.id, GeoPoint(it.latitude, it.longitude)) },
            overlay = MapOverlay(self = self),
            move = move,
            markerContent = { marker ->
                val friend = state.friends.first { it.user.id == marker.id }
                Box(Modifier.clickable { selected = friend }) {
                    FriendPin(friend = friend, selected = selected?.user?.id == friend.user.id)
                }
            },
        )
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            SearchCard(
                query = query,
                open = searchOpen && filtered.isNotEmpty(),
                results = filtered,
                onQuery = {
                    query = it
                    searchOpen = true
                },
                onClear = {
                    query = ""
                    searchOpen = false
                },
                onSelect = { friend ->
                    query = ""
                    searchOpen = false
                    camera = MapCamera(friend.latitude, friend.longitude, 16.0)
                    move += 1
                    selected = friend
                },
            )
            if (!searchOpen && state.friends.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.friends.forEach { friend ->
                        FriendChip(friend) {
                            camera = MapCamera(friend.latitude, friend.longitude, 16.0)
                            move += 1
                            selected = friend
                        }
                    }
                }
            }
        }
        MapControls(
            satellite = satellite,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            onZoomIn = {
                camera = camera.copy(zoom = (camera.zoom + 1).coerceAtMost(18.0))
                move += 1
            },
            onZoomOut = {
                camera = camera.copy(zoom = (camera.zoom - 1).coerceAtLeast(2.0))
                move += 1
            },
            onToggleStyle = { satellite = !satellite },
            onRecenter = {
                self?.let {
                    camera = MapCamera(it.latitude, it.longitude, 15.0)
                    move += 1
                }
            },
        )
        OnlinePill(
            count = state.friends.size,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp),
        )
        if (access == LocationAccess.Unknown || access == LocationAccess.Denied) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)), contentAlignment = Alignment.Center) {
                LocationPermissionCard(
                    access = access,
                    onAllow = onAllowLocation,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
    selected?.let { friend ->
        FriendSheet(
            friend = friend,
            self = self,
            onClose = { selected = null },
            onOpenHistory = {
                selected = null
                onOpenHistory(friend.user.toPerson())
            },
        )
    }
}

@Composable
private fun SearchCard(
    query: String,
    open: Boolean,
    results: List<FriendLocation>,
    onQuery: (String) -> Unit,
    onClear: () -> Unit,
    onSelect: (FriendLocation) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.96f)),
    ) {
        Row(
            modifier = Modifier.height(48.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.search, contentDescription = null, tint = AppColors.slate400, modifier = Modifier.size(20.dp))
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = AppColors.text, fontSize = 15.sp),
                cursorBrush = SolidColor(AppColors.primary),
                singleLine = true,
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(stringResource(Res.string.search_friends), color = AppColors.slate400, fontSize = 15.sp)
                        }
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(AppIcons.close, contentDescription = stringResource(Res.string.close), tint = AppColors.slate400)
                }
            }
        }
        if (open) {
            HorizontalDivider(color = AppColors.hairline)
            results.forEach { friend ->
                val name = displayName(friend.user.username, friend.user.login, friend.user.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(friend) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppAvatar(
                        initials = initialsOf(name),
                        color = avatarColor(friend.user.id),
                        imageUrl = friend.user.avatarUrl,
                        size = 36.dp,
                    )
                    Text(
                        text = name,
                        modifier = Modifier.padding(start = 12.dp),
                        color = AppColors.text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendChip(friend: FriendLocation, onClick: () -> Unit) {
    val name = displayName(friend.user.username, friend.user.login, friend.user.id)
    Row(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .clickable(onClick = onClick)
            .padding(start = 5.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppAvatar(initials = initialsOf(name), color = avatarColor(friend.user.id), imageUrl = friend.user.avatarUrl, size = 26.dp)
        Text(
            text = name.substringBefore(' '),
            modifier = Modifier.padding(start = 6.dp),
            color = AppColors.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MapControls(
    satellite: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onToggleStyle: () -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.End,
    ) {
        ControlButton(AppIcons.add, onZoomIn)
        ControlButton(AppIcons.remove, onZoomOut)
        ControlButton(AppIcons.layers, onToggleStyle, highlighted = satellite)
        ControlButton(AppIcons.myLocation, onRecenter)
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    highlighted: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(if (highlighted) 0.dp else 8.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlighted) AppColors.primarySoft else Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (highlighted) AppColors.primary else AppColors.text, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun OnlinePill(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.text)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(AppColors.online))
        Text(
            text = pluralStringResource(Res.plurals.friends_on_map, count, count),
            modifier = Modifier.padding(start = 8.dp),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendSheet(
    friend: FriendLocation,
    self: GeoPoint?,
    onClose: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val name = displayName(friend.user.username, friend.user.login, friend.user.id)
    val distance = distanceLabel(self, GeoPoint(friend.latitude, friend.longitude))
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = Color.White,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SheetHandle() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onOpenHistory, modifier = Modifier.clip(CircleShape).background(AppColors.primarySoft)) {
                    Icon(AppIcons.route, contentDescription = stringResource(Res.string.location_history), tint = AppColors.primary)
                }
                IconButton(onClick = onClose, modifier = Modifier.padding(start = 8.dp).clip(CircleShape).background(AppColors.hairline)) {
                    Icon(AppIcons.close, contentDescription = stringResource(Res.string.close), tint = AppColors.muted)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onOpenHistory)) {
                AppAvatar(initials = initialsOf(name), color = avatarColor(friend.user.id), imageUrl = friend.user.avatarUrl, size = 60.dp, live = true)
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(name, color = AppColors.text, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (distance != null) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.primarySoft)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(distance, color = AppColors.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(stringResource(Res.string.away), color = AppColors.muted, fontSize = 10.sp)
                    }
                }
            }
            Column(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.canvas)
                    .padding(16.dp),
            ) {
                Row {
                    CoordCell(stringResource(Res.string.latitude), "${friend.latitude.toString().take(8)}°", Modifier.weight(1f))
                    CoordCell(stringResource(Res.string.longitude), "${friend.longitude.toString().take(8)}°", Modifier.weight(1f))
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = AppColors.border)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.schedule, contentDescription = null, tint = AppColors.slate400, modifier = Modifier.size(14.dp))
                    Text(relativeTime(friend.updatedAt), modifier = Modifier.padding(start = 4.dp), color = AppColors.muted, fontSize = 12.sp)
                }
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SheetAction(
                    icon = AppIcons.navigation,
                    label = stringResource(Res.string.navigate),
                    background = AppColors.successSoft,
                    color = AppColors.online,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        openExternalUrl("https://www.google.com/maps/dir/?api=1&destination=${friend.latitude},${friend.longitude}")
                    },
                )
                SheetAction(
                    icon = AppIcons.route,
                    label = stringResource(Res.string.location_history),
                    background = AppColors.canvas,
                    color = AppColors.primary,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenHistory,
                )
            }
        }
    }
}

@Composable
private fun CoordCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), color = AppColors.slate400, fontSize = 11.sp, letterSpacing = 0.5.sp)
        Text(value, color = AppColors.text, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    background: Color,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(label, modifier = Modifier.padding(start = 8.dp), color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
