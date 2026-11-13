package top.krushiler.tracktalk.di

import org.koin.dsl.module
import top.krushiler.tracktalk.config.AppConfig
import top.krushiler.tracktalk.database.GmsDatabase

internal fun databaseModule(config: AppConfig) = module {
    single { config }
    single { GmsDatabase(config.databaseUrl) }
}
