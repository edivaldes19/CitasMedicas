package com.manuel.citasmedicas.utils

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputEditText
import com.manuel.citasmedicas.R

class TextWatchers {
    companion object {
        fun validateFieldsAsYouType(
            context: Context,
            vararg textInputEditText: TextInputEditText
        ) {
            textInputEditText.forEach { text ->
                text.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (s.isNullOrEmpty()) {
                            text.error = context.getString(R.string.this_field_is_required)
                        } else {
                            text.error = null
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })
            }
        }
    }
}