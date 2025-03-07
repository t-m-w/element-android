/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.list.home

import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import im.vector.app.core.platform.StateView
import im.vector.app.core.utils.createUIHandler
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.home.room.list.RoomListListener
import im.vector.app.features.home.room.list.RoomSummaryItemFactory
import im.vector.app.features.home.room.list.RoomSummaryItemPlaceHolder_
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class HomeFilteredRoomsController @Inject constructor(
        private val roomSummaryItemFactory: RoomSummaryItemFactory,
) : PagedListEpoxyController<RoomSummary>(
        // Important it must match the PageList builder notify Looper
        modelBuildingHandler = createUIHandler()
) {

    private var roomChangeMembershipStates: Map<String, ChangeMembershipState>? = null
        set(value) {
            field = value
            // ideally we could search for visible models and update only those
            requestForcedModelBuild()
        }

    var listener: RoomListListener? = null

    private var emptyStateData: StateView.State.Empty? = null
    private var currentState: StateView.State = StateView.State.Content

    override fun addModels(models: List<EpoxyModel<*>>) {
        if (models.isEmpty() && emptyStateData != null) {
            emptyStateData?.let { emptyState ->
                roomListEmptyItem {
                    id("state_item")
                    emptyData(emptyState)
                }
                currentState = emptyState
            }
        } else {
            currentState = StateView.State.Content
            super.addModels(models)
        }
    }

    fun submitEmptyStateData(state: StateView.State.Empty?) {
        this.emptyStateData = state
    }

    override fun buildItemModel(currentPosition: Int, item: RoomSummary?): EpoxyModel<*> {
        item ?: return RoomSummaryItemPlaceHolder_().apply { id(currentPosition) }
        return roomSummaryItemFactory.create(item, roomChangeMembershipStates.orEmpty(), emptySet(), RoomListDisplayMode.ROOMS, listener)
    }
}
