package com.bolao.copa2026.data.di

import com.bolao.copa2026.data.repository.*
import com.bolao.copa2026.domain.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindGroupRepository(impl: GroupRepositoryImpl): GroupRepository

    @Binds
    @Singleton
    abstract fun bindMatchRepository(impl: MatchRepositoryImpl): MatchRepository

    @Binds
    @Singleton
    abstract fun bindBetRepository(impl: BetRepositoryImpl): BetRepository

    @Binds
    @Singleton
    abstract fun bindRankingRepository(impl: RankingRepositoryImpl): RankingRepository
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        return db
    }
}
