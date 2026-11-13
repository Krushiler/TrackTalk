package top.krushiler.tracktalk.config

import kotlin.test.Test
import kotlin.test.assertEquals

class DotEnvTest {
    @Test
    fun parseDotEnvKeepsValuesAndSkipsComments() {
        val parsed = parseDotEnv(
            """
            # comment
            DATABASE_URL=postgresql+asyncpg://postgres:postgres@localhost:5432/gms
            export JWT_SECRET="change-me"
            EMPTY=
            not a pair
            """.trimIndent(),
        )

        assertEquals(
            mapOf(
                "DATABASE_URL" to "postgresql+asyncpg://postgres:postgres@localhost:5432/gms",
                "JWT_SECRET" to "change-me",
                "EMPTY" to "",
            ),
            parsed,
        )
    }
}
