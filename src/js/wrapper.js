export async function clingo(options = {}) {
    let clingo = (await import(/* webpackChunkName: "clingo" */ "./clingo.js")).default;
    let wasm_module = await import(/* webpackChunkName: "wasmName" */ "./clingo.wasm");
    let wasm_name = wasm_module.default;
    options['locateFile'] = function (path) {
            if (path.endsWith('.wasm')) {
                return wasm_name;
            }
            return path;
        };
    let instance = new Promise(function(resolve, reject) {
        clingo(options).then(m => {
            delete m['then'];
            resolve(m);
        });
    });
    return instance;
}

export function run(clingo, asp, args = 0) {
    clingo.ccall('run', 'number', ['string', 'string'], [asp, args]);
}