package com.getir.patika.foodcouriers

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    //private lateinit var tabLayout: TabLayout
    //private lateinit var viewPager2: ViewPager2
    //private lateinit var pagerAdapter: PagerAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById(R.id.button_maps)
        val buttonAuto: Button = findViewById(R.id.button_maps_autocompleteBar)

        button.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("autoSearchBarBool",false)
            startActivity(intent)
        }
        buttonAuto.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("autoSearchBarBool",true)
            startActivity(intent)
        }

        //Account tab
        /*setContentView(R.layout.activity_maps)
        tabLayout = findViewById(R.id.tab_account)
        viewPager2 = findViewById(R.id.viewpager_account)
        pagerAdapter = PagerAdapter(supportFragmentManager,lifecycle).apply {
            addFragment(CreateAccountFragment())
            addFragment(LoginAccountFragment())
        }
        viewPager2.adapter = pagerAdapter

        TabLayoutMediator(tabLayout,viewPager2){ tab, position ->
             when(position) {
                 0 -> {
                     tab.text = "Create Account"
                 }
                 1 -> {
                     tab.text = "Login Account"
                 }
             }
        }.attach()
*/
    }
}