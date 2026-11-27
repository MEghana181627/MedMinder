@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package uk.ac.tees.mad.s3640901.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.ac.tees.mad.s3640901.api.MedicineRepository
import uk.ac.tees.mad.s3640901.data.MedicationDao
import uk.ac.tees.mad.s3640901.data.MedicationEntity
import uk.ac.tees.mad.s3640901.ui.screens.AddMedicationScreen
import uk.ac.tees.mad.s3640901.ui.screens.AuthScreen
import uk.ac.tees.mad.s3640901.ui.screens.DashboardScreen
import uk.ac.tees.mad.s3640901.ui.screens.HistoryScreen

private object Routes {
    const val AUTH = "auth"
    const val DASHBOARD = "dashboard"
    const val ADD = "add"
    const val HISTORY = "history"
    const val EDIT = "edit/{medicationId}"

    fun editMedication(id: Long): String {
        return "edit/$id"
    }
}

@Composable
fun MedMinderNavGraph(
    medicationDao: MedicationDao,
    @Suppress("UNUSED_PARAMETER")
    medicineRepository: MedicineRepository,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.AUTH
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.AUTH) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                medicationDao = medicationDao,
                onAddMedication = {
                    navController.navigate(Routes.ADD)
                },
                onOpenHistory = {
                    navController.navigate(Routes.HISTORY)
                },
                onEditMedication = { medicationId ->
                    navController.navigate(
                        Routes.editMedication(medicationId)
                    )
                }
            )
        }

        composable(Routes.ADD) {
            AddMedicationScreen(
                medicationDao = medicationDao,
                medication = null,
                onBack = {
                    navController.popBackStack()
                },
                onSaved = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                medicationDao = medicationDao,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument("medicationId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->

            val medicationId =
                backStackEntry.arguments
                    ?.getLong("medicationId")
                    ?: -1L

            if (medicationId <= 0L) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Invalid medication")
                }

                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            } else {
                EditMedicationDestination(
                    medicationDao = medicationDao,
                    medicationId = medicationId,
                    onBack = {
                        navController.popBackStack()
                    },
                    onSaved = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun EditMedicationDestination(
    medicationDao: MedicationDao,
    medicationId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var medication by remember(medicationId) {
        mutableStateOf<MedicationEntity?>(null)
    }

    var isLoading by remember(medicationId) {
        mutableStateOf(true)
    }

    LaunchedEffect(medicationId) {
        medication = withContext(Dispatchers.IO) {
            medicationDao.getMedicationById(medicationId)
        }

        isLoading = false
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        medication == null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Medication not found")
            }
        }

        else -> {
            AddMedicationScreen(
                medicationDao = medicationDao,
                medication = medication,
                onBack = onBack,
                onSaved = onSaved
            )
        }
    }
}