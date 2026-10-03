package dev.dozingduckling.besur

import android.content.ContentResolver
import android.provider.MediaStore

class MusicRepository(private val contentResolver: ContentResolver)
{
    fun getMusic(): List<Music>
    {
        val music = mutableListOf<Music>()

        val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

            while (cursor.moveToNext())
            {
                music.add(
                        Music(
                                id = cursor.getLong(idColumn),
                                title = cursor.getString(titleColumn),
                                artist = cursor.getString(artistColumn),
                                album = cursor.getString(albumColumn)
                        )
                )
            }
        }

        return music
    }
}