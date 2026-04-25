使用本地构建而非Docker构建
./backend/target 下构建jar包
```aiignore
mvn clean package -DskipTests
```
./frontend/dist 下构建前端静态资源
```aiignore
npm ci
npm run build
```
