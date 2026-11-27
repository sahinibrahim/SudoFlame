/*
 * SudoFlame reklam ayarları (Google AdSense).
 * AdSense hesabı onaylanınca yalnızca aşağıdaki değerleri doldurmak yeter:
 *   CLIENT : yayıncı kimliği, ör. "ca-pub-1234567890123456"
 *   SLOTS  : AdSense'te oluşturulan reklam birimlerinin "data-ad-slot" numaraları.
 *            Boş bırakılırsa o alan gösterilmez (Otomatik reklamlar açıksa
 *            Google reklamları kendisi yerleştirir).
 * CLIENT boşken hiçbir reklam kodu yüklenmez ve reklam alanları gizli kalır.
 */
(function () {
  'use strict';
  var CLIENT = '';
  var SLOTS = { home: '', lesson: '', daily: '' };

  if (!/^ca-pub-\d{10,20}$/.test(CLIENT)) return;

  var s = document.createElement('script');
  s.async = true;
  s.crossOrigin = 'anonymous';
  s.src = 'https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=' + CLIENT;
  document.head.appendChild(s);

  var list = document.querySelectorAll('.ad-slot[data-slot]');
  for (var i = 0; i < list.length; i++) {
    var box = list[i], id = SLOTS[box.getAttribute('data-slot')];
    if (!id) continue;
    var ins = document.createElement('ins');
    ins.className = 'adsbygoogle';
    ins.style.display = 'block';
    ins.setAttribute('data-ad-client', CLIENT);
    ins.setAttribute('data-ad-slot', id);
    ins.setAttribute('data-ad-format', 'auto');
    ins.setAttribute('data-full-width-responsive', 'true');
    box.appendChild(ins);
    box.hidden = false;
    (window.adsbygoogle = window.adsbygoogle || []).push({});
  }
})();
