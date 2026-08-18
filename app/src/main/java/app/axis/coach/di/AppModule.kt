package app.axis.coach.di

import android.content.Context
import androidx.room.Room
import app.axis.coach.data.local.AxisDatabase
import app.axis.coach.data.local.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AxisDatabase =
        Room.databaseBuilder(context, AxisDatabase::class.java, "axis.db").build()

    @Provides
    fun provideSessionDao(db: AxisDatabase): SessionDao = db.sessionDao()
}
