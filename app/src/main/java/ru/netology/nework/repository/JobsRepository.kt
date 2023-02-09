package ru.netology.nework.repository

import ru.netology.nework.models.jobs.Job

interface JobsRepository {
    suspend fun getMyJobs(): List<Job>
    suspend fun saveMyJob(job: Job): Job
    suspend fun removeMyJobById(id: Long)
    suspend fun getUserJobs(userId: Long): List<Job>
}