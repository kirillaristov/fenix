/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.topsites

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import mozilla.components.feature.top.sites.TopSite
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.databinding.TopSiteItemBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.loadIntoView
import org.mozilla.fenix.home.sessioncontrol.TopSiteInteractor
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.utils.view.ViewHolder

class TopSiteItemViewHolder(
    view: View,
    private val interactor: TopSiteInteractor
) : ViewHolder(view) {
    private lateinit var topSite: TopSite
    private val binding = TopSiteItemBinding.bind(view)

    init {
        binding.topSiteItem.setOnClickListener {
            interactor.onSelectTopSite(topSite)
        }

        binding.topSiteItem.setOnLongClickListener {
            interactor.onTopSiteMenuOpened()
            it.context.components.analytics.metrics.track(Event.TopSiteLongPress(topSite))

            val topSiteMenu = TopSiteItemMenu(
                context = view.context,
                isPinnedSite = topSite is TopSite.Pinned || topSite is TopSite.Default
            ) { item ->
                when (item) {
                    is TopSiteItemMenu.Item.OpenInPrivateTab -> interactor.onOpenInPrivateTabClicked(
                        topSite
                    )
                    is TopSiteItemMenu.Item.RenameTopSite -> interactor.onRenameTopSiteClicked(
                        topSite
                    )
                    is TopSiteItemMenu.Item.RemoveTopSite -> interactor.onRemoveTopSiteClicked(
                        topSite
                    )
                }
            }
            val menu = topSiteMenu.menuBuilder.build(view.context).show(anchor = it)
            it.setOnTouchListener @SuppressLint("ClickableViewAccessibility") { v, event ->
                onTouchEvent(v, event, menu)
            }
            true
        }
    }

    fun bind(topSite: TopSite) {
        binding.topSiteTitle.text = topSite.title

        if (topSite is TopSite.Pinned || topSite is TopSite.Default) {
            val pinIndicator = getDrawable(itemView.context, R.drawable.ic_new_pin)
            binding.topSiteTitle.setCompoundDrawablesWithIntrinsicBounds(pinIndicator, null, null, null)
        } else {
            binding.topSiteTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }

        when (topSite.url) {
            SupportUtils.POCKET_TRENDING_URL -> {
                binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_pocket))
            }
            SupportUtils.BAIDU_URL -> {
                binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_baidu))
            }
            SupportUtils.JD_URL -> {
                binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_jd))
            }
            SupportUtils.PDD_URL -> {
                binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_pdd))
            }
            SupportUtils.TC_URL -> {
                binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_tc))
            }
            SupportUtils.MEITUAN_URL -> {
                binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_meituan))
            }
            else -> {
                itemView.context.components.core.icons.loadIntoView(binding.faviconImage, topSite.url)
            }
        }

        this.topSite = topSite
    }

    private fun onTouchEvent(
        v: View,
        event: MotionEvent,
        menu: PopupWindow
    ): Boolean {
        if (event.action == MotionEvent.ACTION_CANCEL) {
            menu.dismiss()
        }
        return v.onTouchEvent(event)
    }

    companion object {
        const val LAYOUT_ID = R.layout.top_site_item
    }
}

class TopSiteItemMenu(
    private val context: Context,
    private val isPinnedSite: Boolean,
    private val onItemTapped: (Item) -> Unit = {}
) {
    sealed class Item {
        object OpenInPrivateTab : Item()
        object RenameTopSite : Item()
        object RemoveTopSite : Item()
    }

    val menuBuilder by lazy { BrowserMenuBuilder(menuItems) }

    private val menuItems by lazy {
        listOfNotNull(
            SimpleBrowserMenuItem(
                context.getString(R.string.bookmark_menu_open_in_private_tab_button)
            ) {
                onItemTapped.invoke(Item.OpenInPrivateTab)
            },
            if (isPinnedSite) SimpleBrowserMenuItem(
                context.getString(R.string.rename_top_site)
            ) {
                onItemTapped.invoke(Item.RenameTopSite)
            } else null,
            SimpleBrowserMenuItem(
                if (isPinnedSite) {
                    context.getString(R.string.remove_top_site)
                } else {
                    context.getString(R.string.delete_from_history)
                }
            ) {
                onItemTapped.invoke(Item.RemoveTopSite)
            }
        )
    }
}
