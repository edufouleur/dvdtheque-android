package fr.dvdtheque.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun statusToString(value: MovieStatus): String = value.name
    @TypeConverter fun stringToStatus(value: String): MovieStatus = MovieStatus.valueOf(value)
}
