package top.krushiler.tracktalk.di

import org.koin.core.module.Module

fun appModules(baseUrl: String, appBuild: Int = 1): List<Module> = listOf(
    networkModule(baseUrl, appBuild),
    repositoryModule(),
    platformModule(),
    viewModelModule(baseUrl),
)
