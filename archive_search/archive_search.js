var blobField = 'content';
var titleField = 'path';
var bytesField = 'size';
var typeField = 'contentType';

function callback(data) {
  var lilyEndpoint = 'http://localhost:12060/repository/record/';
  var lilyNamespace = 'org.eu.eark';
  
  var resultMessage = data.response.numFound + ' result';
  if (data.response.numFound != 1)
    resultMessage += 's';
  resultMessage += ' found';
  document.getElementById('output').innerHTML = resultMessage;
  
  var searchResults = '';
  for (doc of data.response.docs) {
    var fileAdress = lilyEndpoint + encodeURIComponent(doc['lily.id']) +
        '/field/n$' + blobField + '/data?ns.n=' + lilyNamespace;
    var link = '<a href="' + fileAdress + '">' + doc[titleField] + '</a>';
    var bytes = doc[bytesField];
    var filesize;
    if (bytes < 1024)
      filesize = bytes + ' B';
    else if (bytes < 1024 * 1024)
      filesize = Math.floor(bytes / 1024) + ' kB';
    else
      filesize = Math.floor(bytes / (1024 * 1024)) + ' MB';
    filesize = '[' + filesize + ']';
    searchResults += '<li>' + link + ' ' + filesize + '</li>';
  }
  document.getElementById('output').innerHTML += '<ul>' + searchResults + '</ul>';
}

function askSolr() {
  var solrEndpoint = 'http://localhost:8983/solr/collection1/';
  var queryString = document.forms.find.queryString.value;
  var blobQuery = '';
  if (queryString)
    blobQuery = blobField + ':' + queryString;
  
  var contentTypesQuery = '';
  for (option of document.forms.find.contentTypes.options) {
    if (option.selected) {
      if (contentTypesQuery) contentTypesQuery += " ";
      contentTypesQuery += typeField + ':"' + option.value + '"';
    }
  }

  var package = document.forms.find.package.value;
  var packageQuery = '';
  if (package) {
    packageQuery += titleField + ':"' + package + '"';
  }
  
  var query = '';
  if (blobQuery) query = blobQuery;
  if (packageQuery) {
    if (query)
      query += ' AND ' + packageQuery;
    else
      query = packageQuery;
  }
  if (contentTypesQuery) {
    if (query)
      query += ' AND (' + contentTypesQuery + ')';
    else
      query = contentTypesQuery;
  }
  
  if (!query) query = '*:*';
  
  var sort = document.forms.find.sort.value;
  var sortParameter = '';
  if (sort) {
    sortParameter = '&sort=' + sort;
  }
  
  var script = document.createElement('script');
  script.src = solrEndpoint + 'select?q=' + encodeURIComponent(query) +
      sortParameter + '&rows=20&wt=json&json.wrf=callback';
  document.getElementsByTagName('head')[0].appendChild(script);
}
