package me.xiangning.stackcardlayout

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.xiangning.sectionadapter.SimpleItemBinder
import com.xiangning.sectionadapter.core.SectionAdapter
import jp.wasabeef.recyclerview.animators.SlideInDownAnimator
import me.xiangning.stackcard.library.LandingItemAnimator
import me.xiangning.stackcard.library.StackCardLayoutManager
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val content = findViewById<RecyclerView>(R.id.content)
        content.layoutManager = StackCardLayoutManager()
        content.itemAnimator = LandingItemAnimator()

        content.adapter = SectionAdapter().apply {
            val section = register(0, Integer::class.java, SimpleItemBinder(viewProvider = { inflater, parent ->
                FrameLayout(inflater.context)
            }) { holder, color ->
                holder.itemView.setBackgroundColor(color.toInt())
            })

            content.postDelayed({
              section.setItems((1..10).map { (Random.nextInt() or (0xFF shl 24)) })
            }, 0)
        }
    }
}