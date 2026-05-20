package com.alarmapp.di

import android.content.Context
import com.alarmapp.data.local.AlarmDatabase
import com.alarmapp.data.local.dao.AlarmDao
import com.alarmapp.data.local.dao.PublicHolidayDao
import com.alarmapp.data.repository.AlarmRepositoryImpl
import com.alarmapp.data.repository.HolidayRepositoryImpl
import com.alarmapp.domain.repository.AlarmRepository
import com.alarmapp.domain.repository.HolidayRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAlarmRepository(impl: AlarmRepositoryImpl): AlarmRepository

    @Binds
    @Singleton
    abstract fun bindHolidayRepository(impl: HolidayRepositoryImpl): HolidayRepository

    companion object {

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AlarmDatabase {
            return AlarmDatabase.getInstance(context)
        }

        @Provides
        fun provideAlarmDao(database: AlarmDatabase): AlarmDao {
            return database.alarmDao()
        }

        @Provides
        fun provideHolidayDao(database: AlarmDatabase): PublicHolidayDao {
            return database.publicHolidayDao()
        }
    }
}
