# wuliu-pics

五柳檔案管理腳本（圖片專用版）

主要功能： 1.检查文件损坏 2.备份

不採用 simplemeta, 只記錄 checksum, checked, damaged


## 设计

json 档案与图片在同一资料夹内，方便移动图片。


## 注意

- 圖片與 json 的檔案名稱一一對應，例如 `abc.jpg` 對應 `abc.jpg.json`
- 建議使用 WuliuPicsRename 更改圖片檔案名稱，以便自動更改 json 檔案名稱
- 如果手動更改檔案名稱，請記得同時更改對應的 json 檔案名稱
- 如果 json 檔案找不到對應的圖片檔案，會被刪除 (使用 WuliuPicsOrphan)
- 如果圖片檔案找不到對應的 json 檔案，則自動創建 json 檔案
- 禁止替換檔案

**特別注意**: 禁止替換檔案。  
需要替換檔案時，請先刪除（與同名 json 檔案一起刪除），然後再添加。


## Java Commands

- `javac -cp ".;classes/*" wuliu_pics/common/*.java wuliu_pics/tools/*.java`


## WuliuPicsInit

- 命令: `java -cp ".;classes/*" wuliu_pics.tools.WuliuPicsInit [project-name]`
- 示例: `java -cp ".;classes/*" wuliu_pics.tools.WuliuPicsInit my-project`

說明:

- 本命令用於創建新專案，包括創建必要的資料夾和檔案。
- 執行本命令時需要提供專案名稱。
- 請為每個專案設定不同的名稱，有助於避免備份時搞錯目錄。
- 建議專案名稱不要使用空格。


## WuliuPicsRename

- 小技巧: Ctrl+Shift+C 複製檔案路徑
- 簡單的窗口

## TODO

- 不要數據庫？
  - 不採用 simplemeta, 只記錄 checksum, checked, damaged

