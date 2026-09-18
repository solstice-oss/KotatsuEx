package org.koitharu.kotatsu.massmigrate

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R

class MassMigrateMenuProviderKEx(private val context: Context) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		val item = menu.add(Menu.NONE, MENU_ID, Menu.NONE, R.string.mass_migration)
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		if (menuItem.itemId == MENU_ID) {
			context.startActivity(Intent(context, MassMigrateActivityKEx::class.java))
			return true
		}
		return false
	}

	private companion object {
		const val MENU_ID = 45135
	}
}
