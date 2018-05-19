/*
 * VoIP.ms SMS
 * Copyright (C) 2017 Michael Kourlas
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

package net.kourlas.voipms_sms.preferences.controls

import android.content.Context
import android.content.DialogInterface
import android.support.v7.preference.Preference
import android.util.AttributeSet
import net.kourlas.voipms_sms.R
import net.kourlas.voipms_sms.sms.Database
import net.kourlas.voipms_sms.utils.showAlertDialog
import net.kourlas.voipms_sms.utils.showInfoDialog

/**
 * Preference used to import the database.
 */
@Suppress("unused")
class DatabaseImportPreference : Preference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context,
                                                               attrs)

    constructor(context: Context, attrs: AttributeSet,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onClick() {
        val importPath = Database.getInstance(context).getImportPath()
        if (importPath == null) {
            showInfoDialog(context, context.getString(
                R.string.preferences_database_external_storage_unavailable))
            return
        }

        // Prompt the user before importing the database
        showAlertDialog(
            context,
            context.getString(
                R.string.preferences_database_import_confirm_title),
            context.getString(
                R.string.preferences_database_import_confirm_text,
                importPath.absolutePath),
            context.getString(R.string.ok),
            DialogInterface.OnClickListener { _, _ ->
                try {
                    Database.getInstance(context).import(importPath)
                } catch (e: Exception) {
                    showInfoDialog(context, context.getString(
                        R.string.preferences_database_import_fail),
                                   "${e.message} (${e.javaClass.simpleName})")
                }
            },
            context.getString(R.string.cancel),
            null)
    }
}