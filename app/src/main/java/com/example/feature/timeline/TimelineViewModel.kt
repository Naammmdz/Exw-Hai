package com.example.feature.timeline

import com.example.core.viewmodel.BaseEsmeryViewModel
import com.example.data.EsmeryRepository

class TimelineViewModel(
  repository: EsmeryRepository? = null,
) : BaseEsmeryViewModel(repository ?: com.example.EsmeryServices.repository)
