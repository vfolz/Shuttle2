package com.simplecityapps.shuttle.ui.screens.onboarding.scanner

import com.simplecityapps.mediaprovider.MediaImporter
import com.simplecityapps.mediaprovider.MediaProvider
import com.simplecityapps.mediaprovider.model.Song
import com.simplecityapps.mediaprovider.model.friendlyArtistName
import com.simplecityapps.shuttle.ui.common.mvp.BaseContract
import com.simplecityapps.shuttle.ui.common.mvp.BasePresenter
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.phrase.ListPhrase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Named

interface ScannerContract {

    interface View {
        fun setProgress(providerType: MediaProvider.Type, progress: Int, total: Int, message: String)
        fun setScanStarted(providerType: MediaProvider.Type)
        fun setScanComplete(providerType: MediaProvider.Type, inserts: Int, updates: Int, deletes: Int)
        fun setScanFailed(providerType: MediaProvider.Type)
        fun setAllScansComplete()
        fun dismiss()
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun startScanOrExit()
        fun startScan()
        fun stopScan()
    }
}

class ScannerPresenter @AssistedInject constructor(
    @Named("AppCoroutineScope") private val appCoroutineScope: CoroutineScope,
    private val mediaImporter: MediaImporter,
    @Assisted private val shouldDismissOnScanComplete: Boolean
) : ScannerContract.Presenter,
    BasePresenter<ScannerContract.View>() {

    @AssistedInject.Factory
    interface Factory {
        fun create(shouldDismissOnScanComplete: Boolean): ScannerPresenter
    }

    private var scanJob: Job? = null

    override fun bindView(view: ScannerContract.View) {
        super.bindView(view)

        mediaImporter.listeners.add(listener)
    }

    override fun unbindView() {
        mediaImporter.listeners.remove(listener)
        super.unbindView()
    }


    // Private

    /**
     * It's possible that during onboarding, a scan has completed while the screen was off, so this view was never automatically dismissed.
     * So, if the 'shouldDismissOnScanComplete' flag is set to true, and we've already scanned at least once before, we simply exit.
     * Otherwise, we attempt to scan.
     *
     * Note, if a scan is in progress, this function is a no-op.
     */
    override fun startScanOrExit() {
        if (mediaImporter.isImporting) {
            return
        }

        if (shouldDismissOnScanComplete && mediaImporter.importCount != 0) {
            view?.dismiss()
        } else {
            startScan()
        }
    }

    override fun startScan() {
        stopScan()
        scanJob = appCoroutineScope.launch {
            mediaImporter.import()
        }
    }

    override fun stopScan() {
        scanJob?.cancel()
    }


    // MediaImporter.Listener Implementation

    private val listener = object : MediaImporter.Listener {

        override fun onStart(providerType: MediaProvider.Type) {
            view?.setScanStarted(providerType)
        }

        override fun onProgress(providerType: MediaProvider.Type, progress: Int, total: Int, song: Song) {
            view?.setProgress(providerType, progress, total, ListPhrase.from(" • ").join(listOfNotNull(song.friendlyArtistName, song.name)).toString())
        }

        override fun onComplete(providerType: MediaProvider.Type, inserts: Int, updates: Int, deletes: Int) {
            view?.setScanComplete(providerType, inserts, updates, deletes)
        }

        override fun onAllComplete() {
            view?.setAllScansComplete()
            if (shouldDismissOnScanComplete) {
                view?.dismiss()
            }
        }

        override fun onFail(providerType: MediaProvider.Type) {
            view?.setScanFailed(providerType)
        }
    }
}