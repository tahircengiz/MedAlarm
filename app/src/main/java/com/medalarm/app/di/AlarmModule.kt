package com.medalarm.app.di

import com.medalarm.app.data.alarm.AndroidAlarmRegistrar
import com.medalarm.app.domain.usecase.AlarmRegistrar
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmModule {

    @Binds
    @Singleton
    abstract fun bindAlarmRegistrar(impl: AndroidAlarmRegistrar): AlarmRegistrar
}
