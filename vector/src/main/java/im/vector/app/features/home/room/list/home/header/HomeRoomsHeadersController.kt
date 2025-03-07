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

package im.vector.app.features.home.room.list.home.header

import android.content.res.Resources
import android.util.TypedValue
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.Carousel
import com.airbnb.epoxy.CarouselModelBuilder
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.carousel
import com.google.android.material.color.MaterialColors
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.RoomListListener
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class HomeRoomsHeadersController @Inject constructor(
        val stringProvider: StringProvider,
        private val avatarRenderer: AvatarRenderer,
        resources: Resources,
) : EpoxyController() {

    private var data: RoomsHeadersData = RoomsHeadersData()

    var onFilterChangedListener: ((HomeRoomFilter) -> Unit)? = null
    var recentsRoomListener: RoomListListener? = null
    var invitesClickListener: (() -> Unit)? = null

    private var carousel: Carousel? = null

    private val carouselAdapterObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            if (toPosition == 0 || fromPosition == 0) {
                carousel?.post {
                    carousel?.layoutManager?.scrollToPosition(0)
                }
            }
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            if (positionStart == 0) {
                carousel?.layoutManager?.scrollToPosition(0)
            }
        }
    }

    private val recentsHPadding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            resources.displayMetrics
    ).toInt()

    private val recentsTopPadding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            12f,
            resources.displayMetrics
    ).toInt()

    override fun buildModels() {
        val host = this
        if (data.invitesCount != 0) {
            addInviteCounter(host.invitesClickListener, data.invitesCount)
        }

        data.recents?.let {
            addRecents(host, it)
        }

        host.data.filtersList?.let {
            addRoomFilterHeaderItem(host.onFilterChangedListener, it, host.data.currentFilter)
        }
    }

    private fun addInviteCounter(invitesClickListener: (() -> Unit)?, invitesCount: Int) {
        inviteCounterItem {
            id("invites_counter")
            invitesCount(invitesCount)
            listener { invitesClickListener?.invoke() }
        }
    }

    private fun addRecents(host: HomeRoomsHeadersController, recents: List<RoomSummary>) {
        carousel {
            id("recents_carousel")
            padding(
                    Carousel.Padding(
                            host.recentsHPadding,
                            host.recentsTopPadding,
                            host.recentsHPadding,
                            0,
                            0,
                    )
            )
            onBind { _, view, _ ->
                host.carousel = view

                val colorSurface = MaterialColors.getColor(view, R.attr.vctr_toolbar_background)
                view.setBackgroundColor(colorSurface)

                try {
                    view.adapter?.registerAdapterDataObserver(host.carouselAdapterObserver)
                } catch (e: IllegalStateException) {
                    // do nothing
                }
            }

            onUnbind { _, view ->
                host.carousel = null

                try {
                    view.adapter?.unregisterAdapterDataObserver(host.carouselAdapterObserver)
                } catch (e: IllegalStateException) {
                    // do nothing
                }
            }

            withModelsFrom(recents) { roomSummary ->
                val onClick = host.recentsRoomListener?.let { it::onRoomClicked }
                val onLongClick = host.recentsRoomListener?.let { it::onRoomLongClicked }

                RecentRoomItem_()
                        .id(roomSummary.roomId)
                        .avatarRenderer(host.avatarRenderer)
                        .matrixItem(roomSummary.toMatrixItem())
                        .unreadNotificationCount(roomSummary.notificationCount)
                        .showHighlighted(roomSummary.highlightCount > 0)
                        .itemLongClickListener { _ -> onLongClick?.invoke(roomSummary) ?: false }
                        .itemClickListener { onClick?.invoke(roomSummary) }
            }
        }
    }

    private fun addRoomFilterHeaderItem(
            filterChangedListener: ((HomeRoomFilter) -> Unit)?,
            filtersList: List<HomeRoomFilter>,
            currentFilter: HomeRoomFilter?,
    ) {
        roomFilterHeaderItem {
            id("filter_header")
            filtersData(filtersList)
            selectedFilter(currentFilter)
            onFilterChangedListener(filterChangedListener)
        }
    }

    fun submitData(data: RoomsHeadersData) {
        this.data = data
        requestModelBuild()
    }
}

private inline fun <T> CarouselModelBuilder.withModelsFrom(
        items: List<T>,
        modelBuilder: (T) -> EpoxyModel<*>
) {
    models(items.map { modelBuilder(it) })
}
