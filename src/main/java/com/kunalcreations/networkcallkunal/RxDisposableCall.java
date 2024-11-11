package com.kunalcreations.networkcallkunal;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.view.ViewGroup;
import com.google.gson.Gson;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class RxDisposableCall {
    public static <T> void callApi(
            CompositeDisposable compositeDisposable,
            Observable<T> apiCall,
            Class<T> responseType,
            SuccessCallback<T> onSuccess,
            ErrorCallback onError,
            CompleteCallback onComplete,
            final Dialog loader,
            final Context context) {

        CacheManager<T> cacheManager = new CacheManager<>(context);
        String cacheKey = responseType.getName() + context.getClass();

        // Check for internet connectivity
        if (!isNetworkAvailable(context)) {
            T cachedData = cacheManager.getData(responseType, cacheKey);
            if (cachedData != null) {
                onSuccess.onSuccess(cachedData);
                return;
            } else {
                onError.onError(new Throwable("No internet connection and no cached data available."));
                return;
            }
        }

        // Show loader dialog if applicable
        showLoader(loader, context);

        DisposableObserver<T> disposableObserver = apiCall
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<T>() {
                    @Override
                    public void onNext(T response) {
                        handleApiResponse(response, cacheKey, cacheManager, onSuccess);
                    }

                    @Override
                    public void onError(Throwable e) {
                        onError.onError(e);
                        dismissLoader(loader, context);
                    }

                    @Override
                    public void onComplete() {
                        onComplete.onComplete();
                        dismissLoader(loader, context);
                    }
                });

        compositeDisposable.add(disposableObserver);
    }

    public static <T> void callApi1(
            CompositeDisposable compositeDisposable,
            Observable<T> apiCall,
            Class<T> responseType,
            SuccessCallback<T> onSuccess,
            ErrorCallback onError,
            CompleteCallback onComplete,
            final Context context) {

        CacheManager<T> cacheManager = new CacheManager<>(context);
        String cacheKey = responseType.getName() + context.getClass();

        // Check for internet connectivity
        if (!isNetworkAvailable(context)) {
            T cachedData = cacheManager.getData(responseType, cacheKey);
            if (cachedData != null) {
                onSuccess.onSuccess(cachedData);
                return;
            } else {
                onError.onError(new Throwable("No internet connection and no cached data available."));
                return;
            }
        }

        DisposableObserver<T> disposableObserver = apiCall
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<T>() {
                    @Override
                    public void onNext(T response) {
                        handleApiResponse(response, cacheKey, cacheManager, onSuccess);
                    }

                    @Override
                    public void onError(Throwable e) {
                        onError.onError(e);
                    }

                    @Override
                    public void onComplete() {
                        onComplete.onComplete();
                    }
                });

        compositeDisposable.add(disposableObserver);
    }

    private static <T> void handleApiResponse(T response, String cacheKey, CacheManager<T> cacheManager, SuccessCallback<T> onSuccess) {
        T cachedData = cacheManager.getData((Class<T>) response.getClass(), cacheKey);

        // Only save data if it's new or different
        if (cachedData == null || !isDataEqual(cachedData, response)) {
            onSuccess.onSuccess(response);
            cacheManager.saveData(cacheKey, response); // Save response to cache
        } else {
            onSuccess.onSuccess(cachedData); // Use cached data if no change
        }
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    private static void showLoader(final Dialog loader, final Context context) {
        if (loader != null && !loader.isShowing() && context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                loader.setCancelable(false);
                loader.setCanceledOnTouchOutside(false);
                loader.setContentView(R.layout.progress_bar);
                if (loader.getWindow() != null) {
                    loader.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    loader.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }
                loader.show();
            });
        }
    }

    private static void dismissLoader(final Dialog loader, final Context context) {
        if (loader != null && loader.isShowing() && context instanceof Activity) {
            ((Activity) context).runOnUiThread(loader::dismiss);
        }
    }

    private static <T> boolean isDataEqual(T cachedData, T newData) {
        // Perform deep comparison using Gson
        Gson gson = new Gson();
        String cachedJson = gson.toJson(cachedData);
        String newJson = gson.toJson(newData);
        return cachedJson.equals(newJson);
    }

    public interface SuccessCallback<T> {
        void onSuccess(T response);
    }

    public interface ErrorCallback {
        void onError(Throwable throwable);
    }

    public interface CompleteCallback {
        void onComplete();
    }
}
