var blobField = 'content';
var titleField = 'path';
var bytesField = 'size';

function callback(data) {
  var lilyEndpoint = 'http://localhost:12060/repository/record/';
  var lilyNamespace = 'org.eu.eark';
  
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
  document.getElementById('output').innerHTML = '<ul>' + searchResults + '</ul>';
}

function askSolr() {
  var solrEndpoint = 'http://localhost:8983/solr/collection1/';
  var value = document.forms.find.queryString.value;
  if (!value) value = '*';
  var script = document.createElement('script');
  script.src = solrEndpoint + 'select?q=' + blobField +
      '%3A' + value + '&wt=json&json.wrf=callback';
  document.getElementsByTagName('head')[0].appendChild(script);
}
