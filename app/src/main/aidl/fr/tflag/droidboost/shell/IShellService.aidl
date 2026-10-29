package fr.tflag.droidboost.shell;

import android.os.Bundle;

// Service lancé par Shizuku avec l'identité du shell ADB (uid 2000).
interface IShellService {
    // Code de transaction réservé par Shizuku pour détruire un UserService.
    void destroy() = 16777114;

    // Renvoie un Bundle avec les clés "code" (int), "out" et "err" (String).
    Bundle exec(String command, long timeoutMs) = 1;
}
