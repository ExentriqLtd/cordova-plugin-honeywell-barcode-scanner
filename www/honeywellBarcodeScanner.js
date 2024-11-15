var exec = require('cordova/exec');

module.exports = {
    /**
     * Ascolta gli eventi di scansione dei codici a barre.
     * @param {Function} success - Funzione callback in caso di successo.
     * @param {Function} failure - Funzione callback in caso di errore.
     */
    onBarcodeScanned: function(success, failure) {
        return exec(success, failure, "HoneywellBarcodeScannerPlugin", "onBarcodeScanned", []);
    },

    /**
     * Blocca o sblocca la scansione dei codici a barre.
     * @param {boolean} block - `true` per bloccare, `false` per sbloccare.
     * @param {Function} successCallback - Funzione callback in caso di successo.
     * @param {Function} errorCallback - Funzione callback in caso di errore.
     */
    setScanBlocked: function(block, successCallback, errorCallback) {
        return exec(successCallback, errorCallback, "HoneywellBarcodeScannerPlugin", "setScanBlocked", [block]);
    }
};
