/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import androidx.navigation.NavController
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class HistoryControllerTest {
    private val historyItem = History.Regular(0, "title", "url", 0.toLong(), HistoryItemTimeGroup.timeGroupForTimestamp(0))
    private val scope = TestCoroutineScope()
    private val store: HistoryFragmentStore = mockk(relaxed = true)
    private val state: HistoryFragmentState = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxed = true)
    private val metrics: MetricController = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { store.state } returns state
    }

    @After
    fun cleanUp() {
        scope.cleanupTestCoroutines()
    }

    @Test
    fun onPressHistoryItemInNormalMode() {
        var actualHistoryItem: History? = null
        val controller = createController(
            openInBrowser = {
                actualHistoryItem = it
            }
        )
        controller.handleOpen(historyItem)
        assertEquals(historyItem, actualHistoryItem)
    }

    @Test
    fun onPressHistoryItemInEditMode() {
        every { state.mode } returns HistoryFragmentState.Mode.Editing(setOf())

        createController().handleSelect(historyItem)

        verify {
            store.dispatch(HistoryFragmentAction.AddItemForRemoval(historyItem))
        }
    }

    @Test
    fun onPressSelectedHistoryItemInEditMode() {
        every { state.mode } returns HistoryFragmentState.Mode.Editing(setOf(historyItem))

        createController().handleDeselect(historyItem)

        verify {
            store.dispatch(HistoryFragmentAction.RemoveItemForRemoval(historyItem))
        }
    }

    @Test
    fun onSelectHistoryItemDuringSync() {
        every { state.mode } returns HistoryFragmentState.Mode.Syncing

        createController().handleSelect(historyItem)

        verify(exactly = 0) {
            store.dispatch(HistoryFragmentAction.AddItemForRemoval(historyItem))
        }
    }

    @Test
    fun onBackPressedInNormalMode() {
        every { state.mode } returns HistoryFragmentState.Mode.Normal

        assertFalse(createController().handleBackPressed())
    }

    @Test
    fun onBackPressedInEditMode() {
        every { state.mode } returns HistoryFragmentState.Mode.Editing(setOf())

        assertTrue(createController().handleBackPressed())
        verify {
            store.dispatch(HistoryFragmentAction.ExitEditMode)
        }
    }

    @Test
    fun onModeSwitched() {
        var invalidateOptionsMenuInvoked = false
        val controller = createController(
            invalidateOptionsMenu = {
                invalidateOptionsMenuInvoked = true
            }
        )

        controller.handleModeSwitched()
        assertTrue(invalidateOptionsMenuInvoked)
    }

    @Test
    fun onDeleteAll() {
        var displayDeleteAllInvoked = false
        val controller = createController(
            displayDeleteAll = {
                displayDeleteAllInvoked = true
            }
        )

        controller.handleDeleteAll()
        assertTrue(displayDeleteAllInvoked)
    }

    @Test
    fun onDeleteSome() {
        val itemsToDelete = setOf(historyItem)
        var actualItems: Set<History>? = null
        val controller = createController(
            deleteHistoryItems = { items ->
                actualItems = items
            }
        )

        controller.handleDeleteSome(itemsToDelete)
        assertEquals(itemsToDelete, actualItems)
    }

    @Test
    fun onRequestSync() {
        var syncHistoryInvoked = false
        createController(
            syncHistory = {
                syncHistoryInvoked = true
            }
        ).handleRequestSync()

        coVerifyOrder {
            store.dispatch(HistoryFragmentAction.StartSync)
            store.dispatch(HistoryFragmentAction.FinishSync)
        }

        assertTrue(syncHistoryInvoked)
    }

    @Suppress("LongParameterList")
    private fun createController(
        openInBrowser: (History) -> Unit = { _ -> },
        displayDeleteAll: () -> Unit = {},
        invalidateOptionsMenu: () -> Unit = {},
        deleteHistoryItems: (Set<History>) -> Unit = { _ -> },
        syncHistory: suspend () -> Unit = {}
    ): HistoryController {
        return DefaultHistoryController(
            store,
            navController,
            scope,
            openInBrowser,
            displayDeleteAll,
            invalidateOptionsMenu,
            deleteHistoryItems,
            syncHistory,
            metrics
        )
    }
}
