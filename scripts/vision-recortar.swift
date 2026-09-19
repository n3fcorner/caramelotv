import Foundation
import Vision
import CoreImage
import AppKit

// Levanta o objecto do fundo com o Vision — o mesmo que o Preview faz.
// uso: recortar <entrada> <saida.png>
//
// Devolve o frame INTEIRO com alfa, e nao a caixa do objecto. E' de proposito:
// com a caixa, uma cabeca que sai pela borda do enquadramento fica com um corte
// a direito que so se ve no cartaz final, e nao ha maneira de a detectar — a
// mascara nunca toca a borda da imagem que se recebeu. Em frame inteiro, toca.

guard CommandLine.arguments.count >= 3 else {
    FileHandle.standardError.write("uso: recortar <entrada> <saida.png>\n".data(using: .utf8)!)
    exit(2)
}
let entrada = URL(fileURLWithPath: CommandLine.arguments[1])
let saida = URL(fileURLWithPath: CommandLine.arguments[2])

guard let origem = CIImage(contentsOf: entrada) else {
    FileHandle.standardError.write("nao consegui ler \(entrada.path)\n".data(using: .utf8)!)
    exit(1)
}

let pedido = VNGenerateForegroundInstanceMaskRequest()
let handler = VNImageRequestHandler(ciImage: origem, options: [:])
try handler.perform([pedido])

guard let resultado = pedido.results?.first, !resultado.allInstances.isEmpty else {
    FileHandle.standardError.write("sem objecto detectado\n".data(using: .utf8)!)
    exit(3)
}

// allInstances traz todas as peças em primeiro plano. Fica-se com todas: numa
// foto de cabeças na mão, cada cabeça e' uma instancia.
let buffer = try resultado.generateMaskedImage(
    ofInstances: resultado.allInstances,
    from: handler,
    croppedToInstancesExtent: false
)

let recortada = CIImage(cvPixelBuffer: buffer)
let ctx = CIContext()
guard let cg = ctx.createCGImage(recortada, from: recortada.extent) else { exit(4) }
let rep = NSBitmapImageRep(cgImage: cg)
guard let png = rep.representation(using: .png, properties: [:]) else { exit(5) }
try png.write(to: saida)
print("\(Int(recortada.extent.width))x\(Int(recortada.extent.height))  \(resultado.allInstances.count) instancia(s)")
