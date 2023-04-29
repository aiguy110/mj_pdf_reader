package com.gitlab.mudlej.MjPdfReader.ui.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.gitlab.mudlej.MjPdfReader.R
import com.gitlab.mudlej.MjPdfReader.databinding.ActivityHomeBinding
import com.gitlab.mudlej.MjPdfReader.manager.permission.PermissionManager

class HomeActivity : AppCompatActivity() {

    private lateinit var permissionManager: PermissionManager
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        permissionManager = PermissionManager(this)


        binding.pickFile.setOnClickListener {
            permissionManager.launchPicker()
        }

    }
}