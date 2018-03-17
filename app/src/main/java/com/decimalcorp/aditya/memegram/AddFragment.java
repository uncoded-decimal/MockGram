package com.decimalcorp.aditya.memegram;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;

import static android.app.Activity.RESULT_OK;

public class AddFragment extends Fragment {
    ImageView image;
    Button select, uploads;
    Context context;
    Uri pickedImage;
    TextView textView;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_add_fragment, null);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context=getActivity();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        String s= user.getEmail();
        String x=s.substring(0,s.indexOf('@'));
        mDatabase=mDatabase.child(x);
        final DatabaseReference countLink = mDatabase.child("Count");
        final DatabaseReference uploadLink = mDatabase.child("Uploads");

        image=view.findViewById(R.id.selected);
        select = view.findViewById(R.id.select);
        textView=view.findViewById(R.id.file_uri);
        uploads=view.findViewById(R.id.upload_button);
        uploads.setVisibility(View.INVISIBLE);
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    pickImage();
                }
            });

        final GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(context);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        final StorageReference children = storageRef.child("UserUploads");
        final ProgressDialog dialog=new ProgressDialog(context);
        dialog.setMessage("Uploading...");


        uploads.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!isOnline()){
                 Toast.makeText(context,"No Internet Connection",Toast.LENGTH_SHORT).show();
                    return;
                }

                final StorageReference grandChild = children.child(getFileName(pickedImage));
                StorageMetadata metadata = new StorageMetadata.Builder()
                        .setCustomMetadata("Uploader",acct.getDisplayName())
                        .build();

                final UploadTask uploadTask = grandChild.putFile(pickedImage,metadata);
                dialog.show();
                StorageTask<UploadTask.TaskSnapshot> taskSnapshotStorageTask = uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.setMessage("Upload Failed!");
                        uploads.setVisibility(View.INVISIBLE);
                        dialog.dismiss();
                        Snackbar.make(view.findViewById(R.id.add_fragment), "Upload Failed!", 2000)
                                .show();
                    }
                }).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        dialog.setMessage("Upload Complete!");
                        uploads.setVisibility(View.INVISIBLE);
                        image.setImageDrawable(null);
                        textView.setText("");
                        dialog.dismiss();

                        countLink.setValue(++MainActivity.count);
                                grandChild
                                        .getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        final String s = task.getResult().toString();
                                        Toast.makeText(context, "URI=" + s, Toast.LENGTH_SHORT).show();
                                        uploadLink.child(MainActivity.count + ":")
                                                .setValue(s);
                                        Snackbar.make(view.findViewById(R.id.add_fragment), "Upload Complete!", 1300).show();
                                    }
                                });

                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        int k = (int) (taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount() * 100);
                        if (k > 0) {
                            dialog.setMessage("Uploading..." + k + "%");
                        }
                    }
                });
            }
        });
    }

    private final int REQUEST_IMAGE_GET=9223;

    public void pickImage(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
            startActivityForResult(intent, REQUEST_IMAGE_GET);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode==REQUEST_IMAGE_GET && resultCode==RESULT_OK) {
                Toast.makeText(getActivity(), "Image Selected", Toast.LENGTH_SHORT).show();

                pickedImage = data.getData();
                Picasso.with(context).load(pickedImage)
                        .into(image);
                textView.setText(getFileName(pickedImage));
                uploads.setVisibility(View.VISIBLE);
                select.setText("Pick Another Image");
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if(uri!=null){
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }}
        return result;
    }

    public Boolean isOnline() {
        try {
            Process p1 = java.lang.Runtime.getRuntime().exec("ping -c 1 www.google.com");
            int returnVal = p1.waitFor();
            boolean reachable = (returnVal==0);
            return reachable;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
