package os.kei.ui.page.main.about.page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import os.kei.core.system.ShizukuApiUtils

internal class AboutPageViewModel : ViewModel() {
    private val repository = AboutPageRepository()
    private var detailsJob: Job? = null

    private val _detailsState = MutableStateFlow(AboutPageDetailsState())
    val detailsState: StateFlow<AboutPageDetailsState> = _detailsState.asStateFlow()

    fun refreshDetails(
        context: Context,
        notificationPermissionGranted: Boolean,
        shizukuApiUtils: ShizukuApiUtils
    ) {
        val appContext = context.applicationContext
        detailsJob?.cancel()
        detailsJob = viewModelScope.launch {
            _detailsState.value = repository.loadDetails(
                context = appContext,
                notificationPermissionGranted = notificationPermissionGranted,
                shizukuApiUtils = shizukuApiUtils
            )
        }
    }
}
