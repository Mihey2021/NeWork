package ru.netology.nework.dao

//TODO:
//@Dao
//interface PostDao {
//    @Query("SELECT * FROM PostEntity WHERE notShownYet = 0 ORDER BY id DESC")
//    fun getAll(): Flow<List<PostEntity>>
//
//    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
//    fun getPagingSource(): PagingSource<Int, PostEntity>
//
//    @Query("SELECT * FROM PostEntity WHERE notShownYet = 1 ORDER BY id DESC")
//    fun getNotShownPosts(): List<PostEntity>
//
//    @Query("SELECT * FROM PostEntity WHERE notSaved = 1")
//    suspend fun getNotSavedPosts(): List<PostEntity>
//
//    @Query("Select min(id) from PostEntity WHERE notSaved = 1")
//    suspend fun getMinNotSavePostId(): Long?
//
//    @Query("Select * FROM PostEntity WHERE id = :id AND notSaved = 1")
//    suspend fun getNotSavedPostById(id: Long): PostEntity?
//
//    @Query("SELECT COUNT(*) == 0 FROM PostEntity")
//    suspend fun isEmpty(): Boolean
//
//    @Query("SELECT COUNT(*) FROM PostEntity WHERE notShownYet = 1")
//    suspend fun count(): Int
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insert(post: PostEntity)
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insert(posts: List<PostEntity>)
//
//    @Query("DELETE FROM PostEntity WHERE id = :id")
//    suspend fun removeById(id: Long)
//
//    @Query("DELETE FROM PostEntity")
//    suspend fun clear()
//}

