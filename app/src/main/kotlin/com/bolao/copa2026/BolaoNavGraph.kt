package com.bolao.copa2026

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bolao.copa2026.feature.auth.ChangePasswordScreen
import com.bolao.copa2026.feature.auth.ForgotPasswordScreen
import com.bolao.copa2026.feature.auth.LoginScreen
import com.bolao.copa2026.feature.auth.ProfileScreen
import com.bolao.copa2026.feature.auth.RegisterScreen
import com.bolao.copa2026.feature.bets.BetScreen
import com.bolao.copa2026.feature.groups.CreateGroupScreen
import com.bolao.copa2026.feature.groups.GroupListScreen
import com.bolao.copa2026.feature.groups.JoinGroupScreen
import com.bolao.copa2026.feature.matches.MatchListScreen
import com.bolao.copa2026.feature.ranking.RankingScreen

private object Routes {
    const val REGISTER = "register"
    const val LOGIN = "login"
    const val FORGOT_PASSWORD = "auth/forgot-password"
    const val GROUP_LIST = "groups"
    const val CREATE_GROUP = "groups/create"
    const val JOIN_GROUP = "groups/join"
    const val MATCHES = "groups/{groupId}/matches"
    const val RANKING = "groups/{groupId}/ranking"
    const val BET = "groups/{groupId}/bets/{matchId}"
    const val PROFILE = "auth/profile"
    const val CHANGE_PASSWORD = "auth/change-password"
}

@Composable
fun BolaoNavGraph(
    authViewModel: AppAuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = null)

    if (isLoggedIn == null) return // loading splash

    val start = if (isLoggedIn == true) Routes.GROUP_LIST else Routes.LOGIN

    NavHost(navController = navController, startDestination = start) {

        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.GROUP_LIST) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.GROUP_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.GROUP_LIST) {
            GroupListScreen(
                onNavigateToGroup = { groupId ->
                    navController.navigate("groups/$groupId/matches")
                },
                onNavigateToCreate = { navController.navigate(Routes.CREATE_GROUP) },
                onNavigateToJoin = { navController.navigate(Routes.JOIN_GROUP) },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) }
            )
        }

        composable(Routes.CHANGE_PASSWORD) {
            ChangePasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CREATE_GROUP) {
            CreateGroupScreen(
                onGroupCreated = { groupId ->
                    navController.navigate("groups/$groupId/matches") {
                        popUpTo(Routes.GROUP_LIST)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.JOIN_GROUP) {
            JoinGroupScreen(
                onGroupJoined = { groupId ->
                    navController.navigate("groups/$groupId/matches") {
                        popUpTo(Routes.GROUP_LIST)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.MATCHES,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { back ->
            val groupId = back.arguments?.getString("groupId") ?: ""
            MatchListScreen(
                groupId = groupId,
                onMatchClick = { matchId ->
                    navController.navigate("groups/$groupId/bets/$matchId")
                },
                onRankingClick = { id ->
                    navController.navigate("groups/$id/ranking")
                },
                onGroupDeleted = {
                    navController.navigate(Routes.GROUP_LIST) {
                        popUpTo(Routes.GROUP_LIST) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.BET,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("matchId") { type = NavType.StringType }
            )
        ) {
            BetScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.RANKING,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { back ->
            val groupId = back.arguments?.getString("groupId") ?: ""
            RankingScreen(groupId = groupId)
        }
    }
}
