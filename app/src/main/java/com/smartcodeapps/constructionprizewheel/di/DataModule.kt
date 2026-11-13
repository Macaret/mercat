package com.smartcodeapps.constructionprizewheel.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.smartcodeapps.constructionprizewheel.data.db.AppDatabase
import com.smartcodeapps.constructionprizewheel.data.db.PrizeDao
import com.smartcodeapps.constructionprizewheel.data.db.SpinRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.time.Clock
import javax.inject.Singleton
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "prize_wheel.db").build()

    @Provides
    fun providePrizeDao(db: AppDatabase): PrizeDao = db.prizeDao()

    @Provides
    fun provideSpinRecordDao(db: AppDatabase): SpinRecordDao = db.spinRecordDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            File(context.filesDir, "datastore/settings.preferences_pb")
        }

    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    fun provideRandom(): Random = Random.Default
}
