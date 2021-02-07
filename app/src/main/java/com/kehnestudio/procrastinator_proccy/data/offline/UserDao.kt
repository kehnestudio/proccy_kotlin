package com.kehnestudio.procrastinator_proccy.data.offline

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kehnestudio.procrastinator_proccy.data.offline.online.ScoreHistoryFirestore
import java.time.LocalDate
import java.util.*

@Dao
interface UserDao {

    @Query("SELECT * FROM user_table WHERE userId = :uid")
    fun getSpecificUser(uid: String): LiveData<User>

    @Transaction
    @Query("SELECT SUM(score) FROM score_table where userId =:uid")
    fun getSumOfDailyScore(uid: String): LiveData<Long>

    /** Examples
    @Query("SELECT name FROM user_table WHERE id = :uid")
    fun getUserName(uid: String): LiveData<String>

    @Query("SELECT totalScore FROM user_table WHERE id = :uid")
    fun getTotalScore(uid: String): LiveData<Int>
     **/

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Update
    suspend fun update(user: User)

    @Transaction
    @Query("SELECT score FROM score_table where userId =:uid AND date =:date")
    fun getSpecificDailyScore(uid:String, date: Date): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(scoreHistory: ScoreHistory)

    @Update
    suspend fun updateScore(scoreHistory: ScoreHistory)

    @Query("DELETE FROM user_table")
    suspend fun deleteAllUsers()

    @Query("DELETE FROM score_table")
    suspend fun deleteAllHistory()

    /**
     * FIRESTORE
     */

    @Transaction
    @Query("SELECT date, score FROM score_table where userId =:uid")
    fun getDailyScoreHistory(uid: String): List<ScoreHistoryFirestore>

    @Query("SELECT * FROM user_table WHERE userId = :uid")
    fun getSpecificUserFireStore(uid: String): User

}