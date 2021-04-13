printf "<!DOCTYPE html>
<html lang=\"en\">
<head>
  <meta charset=\"UTF-8\">
  <title>Title</title>
</head>
<body>
 Hello %s
 The content of HTML file. =)
</body>
</html>
" "$1" > index.html
echo "HTML created."
