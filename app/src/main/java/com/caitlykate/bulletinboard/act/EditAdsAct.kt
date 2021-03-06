package com.caitlykate.bulletinboard.act

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.caitlykate.bulletinboard.MainActivity
import com.caitlykate.bulletinboard.R
import com.caitlykate.bulletinboard.adapters.ImageAdapter
import com.caitlykate.bulletinboard.model.Ad
import com.caitlykate.bulletinboard.model.DBManager
import com.caitlykate.bulletinboard.databinding.ActivityEditAdsBinding
import com.caitlykate.bulletinboard.dialogs.DialogSpinnerHelper
import com.caitlykate.bulletinboard.frag.FragmentCloseInterface
import com.caitlykate.bulletinboard.frag.ImageListFrag
import com.caitlykate.bulletinboard.utils.CityHelper
import com.caitlykate.bulletinboard.utils.ImageManager
import com.caitlykate.bulletinboard.utils.ImagePicker
import com.google.android.gms.tasks.OnCompleteListener
import java.io.ByteArrayOutputStream


class EditAdsAct: AppCompatActivity(), FragmentCloseInterface {
    var chooseImageFrag: ImageListFrag? = null
    lateinit var rootElement: ActivityEditAdsBinding
    private val dialog = DialogSpinnerHelper()
    lateinit var imageAdapter: ImageAdapter
    private val dbManager = DBManager()
    private var isEditState = false
    private var ad: Ad? = null  //заполняем если isEditState
    lateinit var newAd: Ad  //заполняем когда нажимаем кнопку "готово"
    private var imageIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootElement = ActivityEditAdsBinding.inflate(layoutInflater)        //инициализируем
        setContentView(rootElement.root)                                    //запускаем
        initAdapters()
        checkEditState()
    }


    private fun checkEditState(){
        isEditState = isEditState()
        if (isEditState){
            ad = intent.getSerializableExtra(MainActivity.ADS_DATA) as Ad
            if (ad != null) fillViews(ad!!)
        }
    }

    private fun isEditState(): Boolean{
        return intent.getBooleanExtra(MainActivity.EDIT_STATE, false)
    }

    private fun fillViews(ad: Ad) = with(rootElement){
        tvCountry.text = ad.country
        tvCity.text = ad.city
        edTel.setText(ad.tel)
        edEmail.setText(ad.email)
        checkBoxWithSend.isChecked = ad.withSend.toBoolean()
        tvCat.text = ad.category
        edTitle.setText(ad.title)
        edPrice.setText(ad.price)
        edDescription.setText(ad.description)
        ImageManager.fillImageArray(ad, imageAdapter)
    }

    private fun initAdapters(){
        imageAdapter = ImageAdapter()
        rootElement.vpImages.adapter = imageAdapter
    }

    ////////////////////onClicks

    fun onClickSelectCountry(view: View){
        val listCountry = CityHelper.getAllCountries(this)
        dialog.showSpinnerDialog(this, listCountry, rootElement.tvCountry)
        if (rootElement.tvCity.text != getString(R.string.select_city)) rootElement.tvCity.setText(
            getString(
                R.string.select_city
            )
        )
    }

    fun onClickSelectCity(view: View){
        val selectedCountry = rootElement.tvCountry.text.toString()
        if (selectedCountry == getString(R.string.select_country)){
            Toast.makeText(this, "Необходимо сначала выбрать страну", Toast.LENGTH_SHORT).show()
        } else {
            val listCities = CityHelper.getAllCities(selectedCountry, this)
            dialog.showSpinnerDialog(this, listCities, rootElement.tvCity)
        }
    }

    fun onClickSelectCat(view: View){
       val listCat = resources.getStringArray(R.array.category).toMutableList() as ArrayList
        dialog.showSpinnerDialog(this, listCat, rootElement.tvCat)
    }

    fun onClickGetImagesOrOpenFrag(view: View) {
        if (imageAdapter.mainArray.size == 0)
            ImagePicker.chooseImages(this,3)
        else {
            //битмапы есть - переносим во фрагмент
            openChooseImageFrag(null)
            chooseImageFrag?.updateAdapterFromEdit(imageAdapter.mainArray)
        }
    }
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    fun onClickPublish(view: View){                     //нажата кнопка "готово"
        newAd = fillAd()
        if (isEditState){           //редактирование
            newAd.copy(key = ad?.key).let { dbManager.publishAd(it, onPublishFinish()) }    //ключ из старого объявления
        } else {                       //публикация
            //dbManager.publishAd(tempAd, onPublishFinish())
            uploadAd()
        }
    }

    //ф-я возвращает интерфейс, кот. мы передаем в функцию publishAd, как только данные опубликовались в базе,
    // запускается addOnCompleteListener, кот. вызывает ф-ю onFinish, инструкция к которой описана здесь
    private fun onPublishFinish(): DBManager.FinishWorkListener{
        return object: DBManager.FinishWorkListener{
            override fun onFinish() {
                finish()
            }

        }
    }


    private fun fillAd(): Ad{
        val adTemp: Ad
        rootElement.apply {
            //нужно будет добавить проверку полей
            adTemp = Ad(tvCountry.text.toString(),
                tvCity.text.toString(),
                edTel.text.toString(),
                edEmail.text.toString(),
                checkBoxWithSend.isChecked.toString(),
                edTitle.text.toString(),
                tvCat.text.toString(),
                edPrice.text.toString(),
                edDescription.text.toString(),
                dbManager.db.push().key,           //генерирует уникальный ключ
                //если написать просто dbManager.db. key, то вернет path (main)
                dbManager.auth.uid,
                "empty","empty","empty",
                System.currentTimeMillis().toString()
            )
        }
        return adTemp
    }

    //запускается, когда возвращаемся из фрагмента редактирования
    //раньше пикс работала на активити, а теперь на фрагменте поверх EditAdsAct,
    //поэтому запускается и в случае закрытия фрагмента выбора фото
    override fun onFragClose(list: ArrayList<Bitmap>) {
        rootElement.scrollViewMain.visibility = View.VISIBLE
        imageAdapter.update(list)
        chooseImageFrag = null
    }
    
    fun openChooseImageFrag(imgList: ArrayList<Uri>?){
        chooseImageFrag = ImageListFrag(this)
        if(imgList != null) chooseImageFrag?.resizeSelectedImages(imgList, true, this)
        rootElement.scrollViewMain.visibility = View.GONE
        val fm = supportFragmentManager.beginTransaction()
        fm.replace(R.id.placeHolder, chooseImageFrag!!)
        fm.commit()
    }

    private fun uploadAd(){
        if (imageAdapter.mainArray.size == imageIndex) {    //нет картинок или уже все картинки загружены в хранилище
            dbManager.publishAd(newAd,onPublishFinish())
            return
        }
        val byteArray = prepareImageByteArray(imageAdapter.mainArray[imageIndex])
        uploadImage(byteArray){     //этот callback запускается как только мы получили ссылку от картинки, что мы загрузили
            nextImage(it.result.toString())
        }
    }

    private fun nextImage(uri: String){
        setImageUriToAd(uri)
        imageIndex++
        uploadAd()
    }

    private fun setImageUriToAd(uri: String){
        when (imageIndex){
            0 -> newAd = newAd.copy(mainImage = uri)
            1 -> newAd = newAd.copy(image2 = uri)
            2 -> newAd = newAd.copy(image3 = uri)
        }
    }

    private fun prepareImageByteArray(bitmap: Bitmap): ByteArray{
        val outStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, outStream)
        return outStream.toByteArray()
    }

    private fun uploadImage(byteArray: ByteArray, listener: OnCompleteListener<Uri>){
        val imStorageRef = dbManager.dbStorage.child(dbManager.auth.uid!!).child("img_${System.currentTimeMillis()}")
        val upTask = imStorageRef.putBytes(byteArray)
        upTask.continueWithTask{
            //скачиваем ссылку картинки, которую только что получили
            task -> imStorageRef.downloadUrl
        }.addOnCompleteListener(listener)
    }

}