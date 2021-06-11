package com.caitlykate.bulletinboard.frag

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caitlykate.bulletinboard.R
import com.caitlykate.bulletinboard.databinding.ListImageFragBinding
import com.caitlykate.bulletinboard.utils.ImagePicker
import com.caitlykate.bulletinboard.utils.ItemTouchMoveCallback

class ImageListFrag(val fragCloseInterface: FragmentCloseInterface, private val imgList: ArrayList<String>): Fragment() {
    lateinit var rootElement: ListImageFragBinding
    val adapter = SelectImageRwAdapter()
    val dragCallback = ItemTouchMoveCallback(adapter)
    val touchHelper = ItemTouchHelper(dragCallback)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //return inflater.inflate(R.layout.list_image_frag, container, false)
        //super.onCreateView(inflater, container, savedInstanceState)
        rootElement = ListImageFragBinding.inflate(inflater)
        return rootElement.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpToolbar()
        //val rcView = view.findViewById<RecyclerView>(R.id.rcViewSelectImage)
        touchHelper.attachToRecyclerView(rootElement.rcViewSelectImage)
        rootElement.rcViewSelectImage.layoutManager = LinearLayoutManager(activity)                    //указываем, что наши элемнеты в РВ будут располагаться друг под другом
        rootElement.rcViewSelectImage.adapter = adapter
        val updateList = ArrayList<SelectImageItem>()
        for (n in 0 until imgList.size){
            //var x = n+1
            updateList.add(SelectImageItem(n.toString(),imgList[n]))
            //если хочу изменить, selectImageItem.copy(title = "123")
        }
        adapter.updateAdapter(updateList)


    }

    override fun onDetach() {
        super.onDetach()
        fragCloseInterface.onFragClose(adapter.mainArray)
        /*Log.d("MyLog", "Title 0: ${adapter.mainArray[0].title}")
        Log.d("MyLog", "Title 1: ${adapter.mainArray[1].title}")
        Log.d("MyLog", "Title 2: ${adapter.mainArray[2].title}")*/
    }

    //настраиваем тулбар: подключаем меню, слушатель нажатий
    private fun setUpToolbar(){
        rootElement.tb.inflateMenu(R.menu.main_choose_image)
        val deleteImageItem = rootElement.tb.menu.findItem(R.id.id_delete_image)
        val addImageItem = rootElement.tb.menu.findItem(R.id.id_add_image)

        //слушатель нажатий для кнопки назад на тулбаре
        rootElement.tb.setNavigationOnClickListener{
            activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()     //закрываем фрагмент
        }

        deleteImageItem.setOnMenuItemClickListener {
            adapter.updateAdapter(ArrayList())
            true
        }
        addImageItem.setOnMenuItemClickListener {
            val imageCount = ImagePicker.MAX_IMAGE_COUNT - adapter.mainArray.size
            ImagePicker.getImages(activity as AppCompatActivity, imageCount)
            true
        }
    }
}