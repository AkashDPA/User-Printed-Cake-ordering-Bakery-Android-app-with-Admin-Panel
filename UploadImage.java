package com.example.homepc.restauranteatitapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.UploadNotificationConfig;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.homepc.restauranteatitapp.Constants.ORDER_ID_IMG_SYNC_URL;

public class UploadImage extends AppCompatActivity implements View.OnClickListener {

//object of databaseHEpler class
    DatabaseHelper mydb;
    //Declaring views
    private Button buttonChoose;
    private Button buttonUpload;
    private ImageView imageView;
    private Button buttonimgCancle;
    private EditText editText;

    //flag to indicate if user has image or not
    Integer isImage =0;
    //varibles to communicate with server
    JSONObject json = null;
    String str = "";
    HttpResponse response;
    Context context = this;

    //Image request code
    private int PICK_IMAGE_REQUEST = 1;

    //storage permission code
    private static final int STORAGE_PERMISSION_CODE = 123;

    //Bitmap to get image from gallery
    private Bitmap bitmap;

    //Uri to store the image uri
    private Uri filePath;

    String orderName, orderQuantity;
    String orderPrice;
@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.img_upload);


    //get order detail parameters from main activity
    Intent i = getIntent();
     orderName= i.getStringExtra("name");
     orderQuantity= i.getStringExtra("quantity");
     orderPrice= i.getStringExtra("price");


    this.mydb=new DatabaseHelper(context.getApplicationContext());

        //Requesting storage permission
        requestStoragePermission();

        //Initializing views
        buttonChoose = (Button) findViewById(R.id.abuttonChoose);
        buttonUpload = (Button) findViewById(R.id.abuttonUpload);
        imageView = (ImageView) findViewById(R.id.aimageView);
        editText = (EditText) findViewById(R.id.aeditTextName);

        buttonimgCancle = (Button) findViewById(R.id.abutton_noimg);
        //Setting clicklistener
        buttonChoose.setOnClickListener(this);
        buttonUpload.setOnClickListener(this);
        buttonimgCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 finish();
               /* Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intent);*/
            }
        });
    }


    @Override
    public void onClick(View v) {
        if (v == buttonChoose) {
            showFileChooser();
        }
        if(v==buttonimgCancle)
        {
            //put order in server
            boolean isinserted = mydb.placeOrder(orderName,orderQuantity,orderPrice.toString());

        }
        if (v == buttonUpload) {

            if(filePath.getPath()!=null)
            {
                uploadMultipart();
                //after image upload call funtion to put order Details in db
                boolean isinserted = mydb.placeOrder(orderName,orderQuantity,orderPrice.toString());
                //copy order id to image db i.e. syncronize
                new orderIdImageIdSync().execute();
            }

             else
                Toast.makeText(getApplicationContext(),"Please Select Image", Toast.LENGTH_SHORT);



        }

    }

    /*
     * This is the method responsible for image upload
     * We need the full image path and the name for the image in this method
     * */
    public void uploadMultipart() {
        //getting name for the image
        String name = editText.getText().toString().trim();

        //getting the actual path of the image
        String path = getPath(filePath);

        {
        //Uploading code
        try {
            String uploadId = UUID.randomUUID().toString();

            //Creating a multi part request
            Toast.makeText(this, "Starting Uploaded!",
                    Toast.LENGTH_SHORT).show();

            new MultipartUploadRequest(this, uploadId, Constants.UPLOAD_URL)
                    .addFileToUpload(path, "image") //Adding file
                    .addParameter("name", name) //Adding text parameter to the request
                    .setNotificationConfig(new UploadNotificationConfig())
                    .setMaxRetries(2)
                    .startUpload(); //Starting the upload
            //after upload
            Toast.makeText(this, "Image Uploaded!",
                    Toast.LENGTH_SHORT).show();

            //set flag to indicate image is uploaded
            isImage = 1;

            //after upload go to main
            finish();

            //  Intent myactivity = new Intent(getApplicationContext(), FastFoodFragment.class);
            //getApplicationContext().startActivity(myactivity);



        } catch (Exception exc) {
            Toast.makeText(this, exc.getMessage(), Toast.LENGTH_SHORT).show();
            //set flag to indicate image is not uploaded due to error
            isImage = 0;

        }

    }}



    //method to show file chooser
    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    //handling the image chooser activity result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageView.setImageBitmap(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //method to get the file path from uri
    public String getPath(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        cursor.close();

        cursor = getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }


    //Requesting permission
    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            return;

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            //If the user has denied the permission previously your code will come to this block
            //Here you can explain why you need this permission
            //Explain here why you need this permission
        }
        //And finally ask for the permission
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
    }


    //This method will be called when the user will tap on allow or deny
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        //Checking the request code of our request
        if (requestCode == STORAGE_PERMISSION_CODE) {

            //If permission is granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Displaying a toast
                Toast.makeText(this, "Permission granted now you can read the storage", Toast.LENGTH_LONG).show();
            } else {
                //Displaying another toast if permission is not granted
                Toast.makeText(this, "Oops you just denied the permission", Toast.LENGTH_LONG).show();
            }
        }
    }




//to imageID and order id sync at sever
    class orderIdImageIdSync extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            //to url parameter yes=go so no human error should generate
            nameValuePairs.add(new BasicNameValuePair("go", "yes"));

            try {
                HttpClient httpClient = new DefaultHttpClient();

                HttpPost httpPost = new HttpPost(ORDER_ID_IMG_SYNC_URL);

                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse response = httpClient.execute(httpPost);

                HttpEntity entity = response.getEntity();

            } catch (ClientProtocolException e) {

            } catch (IOException e) {

            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            // Toast.makeText(MainActivity.this, "Order id Successfully copied to image database", Toast.LENGTH_LONG).show();
        }
    }




    }
