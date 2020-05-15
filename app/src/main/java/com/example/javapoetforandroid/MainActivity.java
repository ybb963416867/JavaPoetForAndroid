package com.example.javapoetforandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.example.annotation_lib.HelloAnnotation;

@HelloAnnotation
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    String str;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, HelloWorld.hello1() + HelloWorld.hello2() + HelloWorld.hello3());
    }
}
