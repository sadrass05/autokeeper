package com.example.autobookkeeper.di

import android.content.Context
import androidx.room.Room
import com.example.autobookkeeper.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "autobookkeeper.db"
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4
        ).build()
    }

    @Provides
    fun provideExpenseDao(database: AppDatabase) = database.expenseDao()

    @Provides
    fun provideFinanceDao(database: AppDatabase) = database.financeDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase) = database.categoryDao()

    @Provides
    fun provideFinanceExpenseDao(database: AppDatabase) = database.financeExpenseDao()
}