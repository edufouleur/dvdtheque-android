package fr.dvdtheque.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Movie::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movies ADD COLUMN watched INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movies ADD COLUMN tmdbId INTEGER")
            }
        }


        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movies ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'movie'")
                db.execSQL("ALTER TABLE movies ADD COLUMN totalSeasons INTEGER")
                db.execSQL("ALTER TABLE movies ADD COLUMN totalEpisodes INTEGER")
                db.execSQL("ALTER TABLE movies ADD COLUMN ownedSeasons TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE movies ADD COLUMN currentSeason INTEGER")
                db.execSQL("ALTER TABLE movies ADD COLUMN currentEpisode INTEGER")
            }
        }

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "dvdtheque.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                .also { INSTANCE = it }
        }
    }
}
