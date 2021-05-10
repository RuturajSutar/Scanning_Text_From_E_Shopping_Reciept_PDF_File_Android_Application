package com.starkindustries.ilabpdfanalyzer;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageScrollListener;
import com.github.barteksc.pdfviewer.listener.OnRenderListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    PDFView pdfView;
    Integer pageNumber = 1;
    private static final int REQUEST_CODE = 10;
    public String path;
    Intent myFileIntent;
    private Context context;
    public static final int CODE = 101;
    EditText product_name;
    DatabaseReference mRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar;
        actionBar = getSupportActionBar();
        ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#E26940"));
        actionBar.setBackgroundDrawable(colorDrawable);

        pdfView = (PDFView)findViewById(R.id.pdfview);

        product_name = (EditText)findViewById(R.id.editText);

        FloatingActionButton fab_3 = findViewById(R.id.fab_3);
        fab_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,Product_List.class);
                startActivity(intent);
            }
        });

        FloatingActionButton fab_2 = findViewById(R.id.fab_2);
        fab_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view1 = getWindow().getDecorView().getRootView();
                view1.setDrawingCacheEnabled(true);
                Bitmap bitmap = Bitmap.createBitmap(view1.getDrawingCache());
                view1.setDrawingCacheEnabled(false);

                String filePath = Environment.getExternalStorageDirectory()+"/DCIM/Screenshots/"+ Calendar.getInstance().getTime().toString()+".jpg";
                File fileScreenshot = new File(filePath);
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(fileScreenshot);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    Toast.makeText(MainActivity.this, "Screenshot is taken", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ActivityCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CODE);


                }
                else {
                    startSearch();
                    Toast.makeText(MainActivity.this, "Select file from storage", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void startSearch(){
        myFileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        myFileIntent.setType("application/pdf");
        myFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(myFileIntent,REQUEST_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){

                startSearch();
                Toast.makeText(MainActivity.this, "Select file from storage", Toast.LENGTH_SHORT).show();

            }
            else {
                Toast.makeText(this, "Storage Permission is Required to access the Files", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        String product = product_name.getText().toString();

        switch (requestCode){

            case 10:

                if (resultCode==RESULT_OK){

                    Uri uri = data.getData();
                    File file = new File(uri.getPath());
                    path = uri.getPath();

//                    Toast.makeText(this, uri.getPath().toString(), Toast.LENGTH_LONG).show();

                    pdfView.fromUri(uri).enableDoubletap(true).load();



                }
                break;

            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                CropImage.ActivityResult result = CropImage.getActivityResult(data);


                if (resultCode == RESULT_OK) {
                    Uri resultUri = result.getUri();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                        TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();
                        if (!recognizer.isOperational()){
                            Toast.makeText(this, "Error in Recognizing", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                            SparseArray<TextBlock> items = recognizer.detect(frame);
                            StringBuilder sb = new StringBuilder();
                            for(int i =0;i<items.size();i++){
                                TextBlock myItem = items.valueAt(i);
                                sb.append(myItem.getValue());
                                sb.append("\n");
                            }
//                            Toast.makeText(MainActivity.this, sb.toString(), Toast.LENGTH_SHORT).show();

                            mRef= FirebaseDatabase.getInstance().getReference().child(product);
                            Map<String,Object> insertValues=new HashMap<>();
                            String key = mRef.push().getKey();
                            insertValues.put("Scanned",sb.toString());
                            mRef.child(key).setValue(insertValues);
                            Toast.makeText(MainActivity.this, "Sanned Successfully", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;

            case CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE:
                Toast.makeText(this, "Error is occured", Toast.LENGTH_SHORT).show();
                break;





        }



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.select_text_options,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        String product = product_name.getText().toString();
        switch (item.getItemId()){

            case R.id.item1:
                if (product.isEmpty()){
                    product_name.setError("Please Enter Product Name");
                    product_name.requestFocus();
                }
                else{
                    Toast.makeText(MainActivity.this, "Select Seller’s Name and adress", Toast.LENGTH_SHORT).show();
                    CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(MainActivity.this);
                }
                return true;

            case R.id.item2:
                if (product.isEmpty()){
                    product_name.setError("Please Enter Product Name");
                    product_name.requestFocus();
                }
                else{
                    Toast.makeText(MainActivity.this, "Select Buyer’s name and address", Toast.LENGTH_SHORT).show();
                    CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(MainActivity.this);
                }
                return true;

            case R.id.item3:
                if (product.isEmpty()){
                    product_name.setError("Please Enter Product Name");
                    product_name.requestFocus();
                }
                else{
                    Toast.makeText(MainActivity.this, "Select Invoice number", Toast.LENGTH_SHORT).show();
                    CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(MainActivity.this);
                }
                return true;

            case R.id.item4:
                if (product.isEmpty()){
                    product_name.setError("Please Enter Product Name");
                    product_name.requestFocus();
                }
                else{
                    Toast.makeText(MainActivity.this, "Select Description of goods", Toast.LENGTH_SHORT).show();
                    CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(MainActivity.this);
                }
                return true;

            case R.id.item5:
                if (product.isEmpty()){
                    product_name.setError("Please Enter Product Name");
                    product_name.requestFocus();
                }
                else{
                    Toast.makeText(MainActivity.this, "Select HSN code", Toast.LENGTH_SHORT).show();
                    CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(MainActivity.this);
                }
                return true;

            case R.id.item6:
                if (product.isEmpty()){
                    product_name.setError("Please Enter Product Name");
                    product_name.requestFocus();
                }
                else{
                    Toast.makeText(MainActivity.this, "Select GST rate", Toast.LENGTH_SHORT).show();
                    CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(MainActivity.this);
                }
                return true;

            case R.id.item7:
                if (product.isEmpty()){
                    product_name.setError("Please Enter Product Name");
                    product_name.requestFocus();
                }
                else{
                    Toast.makeText(MainActivity.this, "Select Quantity", Toast.LENGTH_SHORT).show();
                    CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(MainActivity.this);
                }
                return true;

            case R.id.item8:
                if (product.isEmpty()){
                    product_name.setError("Please Enter Product Name");
                    product_name.requestFocus();
                }
                else{
                    Toast.makeText(MainActivity.this, "Select Rate", Toast.LENGTH_SHORT).show();
                    CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(MainActivity.this);
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

}
