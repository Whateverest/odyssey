/*
 * Copyright (C) 2022 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.gateshipone.odyssey.models.GenericModel

abstract class GenericViewModel<T: GenericModel>(
    application: Application
) : AndroidViewModel(application) {

    val data : MutableLiveData<List<T>?> = MutableLiveData()

    abstract fun loadData()

    fun reloadData() {
        loadData()
    }

    fun clearData() {
        data.value = null
    }

    protected fun setData(data: List<T>?) {
        this.data.value = data
    }
}
