package com.goazzi.workoutmanager.repository

import androidx.lifecycle.LiveData
import com.goazzi.workoutmanager.model.Session
import com.goazzi.workoutmanager.repository.cache.dao.SessionDao
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class SessionRepository(private val sessionDao: SessionDao) {

    suspend fun insert(session: Session) {
        sessionDao.insert(session)
    }

    suspend fun delete(session: Session) {
        sessionDao.delete(session)
    }

    fun getLiveSessionsById(id: String): LiveData<MutableList<Session>> {
        return sessionDao.getLiveSessionsById(id)
    }

    fun getSessionsById(id: String): MutableList<Session> {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val future: Future<MutableList<Session>> = executor.submit(SelectCallable(id, sessionDao))
        return future.get()
    }

    private class SelectCallable(val id: String, val sessionDao: SessionDao) :
        Callable<MutableList<Session>> {

        override fun call(): MutableList<Session> {
            return sessionDao.getSessionsById(id)
        }
    }
}