package com.halimegokmen.audioplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.palette.graphics.Palette;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, androidx.appcompat.widget.SearchView.OnQueryTextListener { //onquerytextlistener: arama yapabilmek için
    public static final int REQUEST_CODE =1;
     static ArrayList<MusicFiles> musicFiles; //static yaparak songsfragmentte kullanımını sağladık
     static  boolean shuffleBoolean = false, repeatBoolean = false;
     static  ArrayList<MusicFiles> albums = new ArrayList<>();
    private  String MY_SORT_PREF = "SortOrder";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permission();

        
    }

    private void permission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) //manifest dosyasına izni yazdıktan sonra burada çağırdık
                != PackageManager.PERMISSION_GRANTED){ //eğer izin verilmemişse, izin isteği soruluyor
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CODE);
        }
        else {

            musicFiles = getAllAudio(this);
            initViewPager();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE){
            if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                // We can do whatever we want permission related;

                musicFiles = getAllAudio(this);
                initViewPager();

            }
            else{
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CODE);
                // izin verilmediği taktirde ekranda sürekli izin isteyecek

            }

        }
    }

    private void initViewPager() {
        ViewPager viewPager = findViewById(R.id.viewpager);
        TabLayout tabLayout = findViewById(R.id.tab_Layout);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter((getSupportFragmentManager()));
        // initializing viewpageradapter, getsupportfragmentmanager: fragmentler arasında iletişimi sağlar.
        viewPagerAdapter.addFragments( new SongsFragment(),"Songs");
        viewPagerAdapter.addFragments(new AlbumsFragment(),"Albums");
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager); //tablayout ile viewpager'ı kuracak(setup)



    }



    public static  class ViewPagerAdapter extends FragmentPagerAdapter {
        private ArrayList<Fragment> fragments;
        private  ArrayList<String> titles;


        public ViewPagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
            this.fragments = new ArrayList<>(); // initializing array list for fragments
            this.titles = new ArrayList<>(); // initializing array list for titles
        }

        void addFragments(Fragment fragment, String title) {

            fragments.add(fragment); // fragment array listesine ekler
            titles.add(title); // title array listesine ekler
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        } //fragment'in pozisyonunu döndürür

        @Override
        public int getCount() {
            return fragments.size();
        } //fragment'in boyutunu döndürür.

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        } //bulunulan konumdaki title'ı döndürecek
    }

    public ArrayList<MusicFiles> getAllAudio(Context context){
         SharedPreferences preferences = getSharedPreferences(MY_SORT_PREF,MODE_PRIVATE); //sıralama için
          String sortOrder = preferences.getString("sorting","sortByName"); //s1: default value we can choose whatever we want. sıralama seçeneği için
        ArrayList<String> duplicate = new ArrayList<>();// albüm kısmında iki kere görüneneler için
        albums.clear();
        ArrayList<MusicFiles> tempAudioList = new ArrayList<>();
        String order = null; //sort order
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI; // uri ile telefondaki müziklere erişeceğiz, yani kaynağı gösterir
       switch (sortOrder){ // we pass sortorder for checking which of the item is selected
            case "sortByName":
                order = MediaStore.MediaColumns.DISPLAY_NAME + " ASC";//ascending order
                break;
            case "sortByDate":
                order = MediaStore.MediaColumns.DATE_ADDED + " ASC";
                break;
            case "sortBySize":
                order = MediaStore.MediaColumns.SIZE  + " DESC";
                break;
        }
        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA, //for path
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID

        };
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, order);
        if( cursor != null){ // cursor ile album,title vb index bilgisini alıyor
            while ((cursor.moveToNext())){
                String album = cursor.getString(0);
                String title = cursor.getString(1);
                String duration = cursor.getString(2);
                String path = cursor.getString(3);
                String artist = cursor.getString(4);
                String id = cursor.getString(5);

                MusicFiles musicFiles = new MusicFiles( path, title,artist,album,duration,id); // musicfiles'ı başlatıyor
                // take log.e for check
                Log.e("Path: " +path,"Album: "+album); //logcat'de telefondan erişilen müzik dosyalarını göreceğiz
                tempAudioList.add(musicFiles);
                if(!duplicate.contains(album)){
                    albums.add(musicFiles);
                    duplicate.add(album);
                }

            }
            cursor.close();
        }
        return tempAudioList;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { // müzikleri aramak için
        getMenuInflater().inflate(R.menu.search,menu); // arama menüsü gösterimi
        MenuItem menuItem = menu.findItem(R.id.search_option);
        androidx.appcompat.widget.SearchView  searchView = (androidx.appcompat.widget.SearchView) menuItem.getActionView(); //arama butonunu searchview sınıfıyla cast ettik, bu şekilde kullanacağız
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        String userInput = s.toLowerCase() ; //aramaya yazdıklarımızı öncelikle küçük harfe çeviriyor
        ArrayList<MusicFiles> myFiles = new ArrayList<>(); // arama yaptığımız kelimelere göre bulunan şarkıları yeni bir diziye atıyor.
        for(MusicFiles song: musicFiles){
            if(song.getTitle().toLowerCase().contains(userInput)){
                myFiles.add(song);
            }
        }

        SongsFragment.musicAdapter.updateList(myFiles); // aranan şarkıların listei güncelleniyor
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences.Editor editor = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE).edit();
        switch (item.getItemId()){
            case R.id.by_name:
                editor.putString("sorting","sortByName");
                editor.apply();
                this.recreate();
                break;
            case  R.id.by_date:
                editor.putString("sorting","sortByDate");
                editor.apply();
                this.recreate();
                break;
            case  R.id.by_size:
                editor.putString("sorting","sortBySize");
                editor.apply();
                this.recreate();
                break;
        }


        return super.onOptionsItemSelected(item);
    }
}