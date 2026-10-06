package com.example.calibretv.data.storage

import android.content.Context
import androidx.room.*

// ─── Entidades ───────────────────────────────────────────────────────────────

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val epubUrl: String?,
    val summary: String,
    val category: String,
    val tags: String, // JSON array: ["tag1","tag2"]
    val shelves: String = "[]", // JSON array: ["shelf1","shelf2"]
    val progressPercent: Int = 0,
    val lastReadSpread: Int = 0
)

@Entity(tableName = "reading_progress", primaryKeys = ["profileId", "bookId"])
data class ReadingProgressEntity(
    val profileId: String,
    val bookId: String,
    val spreadIndex: Int = 0,
    val percent: Int = 0,
    val lastReadAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites", primaryKeys = ["profileId", "bookId"])
data class FavoriteEntity(
    val profileId: String,
    val bookId: String
)

@Entity(tableName = "book_notes")
data class BookNoteEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val profileId: String,
    val noteText: String,
    val spreadIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// ─── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY title ASC")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id IN (:ids)")
    suspend fun getBooksByIds(ids: List<String>): List<BookEntity>

    @Upsert
    suspend fun upsertBooks(books: List<BookEntity>)

    @Query("DELETE FROM books")
    suspend fun clearAll()

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBook(id: String)

    @Query("SELECT COUNT(*) FROM books")
    suspend fun count(): Int
}

@Dao
interface ProgressDao {
    @Query("SELECT spreadIndex FROM reading_progress WHERE profileId=:profileId AND bookId=:bookId")
    suspend fun getSpreadIndex(profileId: String, bookId: String): Int?

    @Query("SELECT percent FROM reading_progress WHERE profileId=:profileId AND bookId=:bookId")
    suspend fun getPercent(profileId: String, bookId: String): Int?

    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)
}

@Dao
interface FavoriteDao {
    @Query("SELECT bookId FROM favorites WHERE profileId=:profileId")
    suspend fun getFavoriteIds(profileId: String): List<String>

    @Query("SELECT COUNT(*)>0 FROM favorites WHERE profileId=:profileId AND bookId=:bookId")
    suspend fun isFavorite(profileId: String, bookId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE profileId=:profileId AND bookId=:bookId")
    suspend fun remove(profileId: String, bookId: String)
}

@Dao
interface BookNoteDao {
    @Query("SELECT * FROM book_notes WHERE bookId=:bookId AND profileId=:profileId ORDER BY createdAt DESC")
    suspend fun getNotes(bookId: String, profileId: String): List<BookNoteEntity>

    @Query("SELECT * FROM book_notes WHERE bookId=:bookId ORDER BY createdAt DESC")
    suspend fun getAllNotesForBook(bookId: String): List<BookNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: BookNoteEntity)

    @Query("DELETE FROM book_notes WHERE id=:noteId")
    suspend fun deleteNote(noteId: String)
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities = [BookEntity::class, ReadingProgressEntity::class, FavoriteEntity::class, BookNoteEntity::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun progressDao(): ProgressDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun bookNoteDao(): BookNoteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * Migraciones explícitas. Al subir `version`, añade aquí una Migration(n, n+1)
         * para conservar progreso de lectura, favoritos y notas de los usuarios.
         * Ejemplo:
         *   val MIGRATION_3_4 = object : Migration(3, 4) {
         *       override fun migrate(db: SupportSQLiteDatabase) {
         *           db.execSQL("ALTER TABLE books ADD COLUMN language TEXT NOT NULL DEFAULT ''")
         *       }
         *   }
         */
        private val MIGRATIONS: Array<androidx.room.migration.Migration> = arrayOf()

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calibrotv.db"
                )
                    .addMigrations(*MIGRATIONS)
                    // Solo se recrea la BD desde esquemas antiguos (v1, v2) o en downgrade.
                    // Desde v3 en adelante, cualquier cambio de esquema exige una Migration explícita.
                    .fallbackToDestructiveMigrationFrom(true, 1, 2)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build().also { INSTANCE = it }
            }
    }
}
