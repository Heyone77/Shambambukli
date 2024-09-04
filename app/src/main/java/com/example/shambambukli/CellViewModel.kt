package com.example.shambambukli

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class CellViewModel : ViewModel() {
    private val _cells = MutableStateFlow<List<CellState>>(emptyList())
    val cells: StateFlow<List<CellState>> = _cells.asStateFlow()

    private var consecutiveAliveCount = 0
    private var consecutiveDeadCount = 0
    private var nextId = 0

    private val MAX_CELLS = 100

    fun addRandomCell() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (_cells.value.size < MAX_CELLS) {
                    val newCell = if (Random.nextBoolean()) {
                        CellState.Alive(getNextId())
                    } else {
                        CellState.Dead(getNextId())
                    }

                    _cells.update { currentCells ->
                        currentCells + newCell
                    }

                    updateCounts(newCell)
                }
            }
        }
    }

    private fun updateCounts(newCell: CellState) {
        viewModelScope.launch(Dispatchers.Default) {
            when (newCell) {
                is CellState.Alive -> {
                    consecutiveAliveCount++
                    consecutiveDeadCount = 0
                }
                is CellState.Dead -> {
                    consecutiveDeadCount++
                    consecutiveAliveCount = 0
                }
                else -> {
                    consecutiveAliveCount = 0
                    consecutiveDeadCount = 0
                }
            }

            if (consecutiveAliveCount == 3) {
                addLife()
                consecutiveAliveCount = 0
            }

            if (consecutiveDeadCount == 3) {
                removeNearestLife()
                consecutiveDeadCount = 0
            }
        }
    }

    private fun addLife() {
        viewModelScope.launch(Dispatchers.IO) {
            _cells.update { currentCells ->
                currentCells + CellState.Life(getNextId())
            }
        }
    }

    private fun removeNearestLife() {
        viewModelScope.launch(Dispatchers.IO) {
            _cells.update { currentCells ->
                val lifeIndex = currentCells.indexOfLast { it is CellState.Life }
                if (lifeIndex != -1) {
                    currentCells.toMutableList().also { it.removeAt(lifeIndex) }
                } else {
                    currentCells
                }
            }
        }
    }

    private fun getNextId(): Int {
        return nextId++
    }
}

sealed class CellState(
    val title: String,
    val subtitle: String,
    val backgroundColor: Color = Color.White,
    val iconResId: Int,
    val uniqueId: Int
) {
    class Dead(uniqueId: Int) : CellState("Мёртвая", "или прикидывается", Color.White, R.drawable.dead, uniqueId)
    class Alive(uniqueId: Int) : CellState("Живая", "и шевелится!", Color.White, R.drawable.alive, uniqueId)
    class Life(uniqueId: Int) : CellState("Жизнь", "Ку-ку!", Color.White, R.drawable.life, uniqueId)
}